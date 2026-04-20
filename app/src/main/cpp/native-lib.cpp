#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <atomic>
#include <mutex>
#include "llama.h"

#define TAG "TypeAssistNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static llama_model * model = nullptr;
static llama_context * ctx = nullptr;
static std::mutex g_mutex;
static std::atomic<bool> g_is_generating(false);
static std::atomic<bool> g_should_abort(false);

// Callback for llama_decode to check if it should stop early
static bool abort_callback(void * data) {
    return g_should_abort.load();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_typeassist_app_service_MyAccessibilityService_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    return env->NewStringUTF(llama_print_system_info());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_typeassist_app_service_MyAccessibilityService_loadModel(
        JNIEnv* env,
        jobject /* this */,
        jstring model_path) {
    
    const char * path = env->GetStringUTFChars(model_path, nullptr);
    LOGD("NATIVE: Loading model: %s", path);

    llama_backend_init();
    LOGD("NATIVE: llama.cpp system info: %s", llama_print_system_info());

    auto mparams = llama_model_default_params();
    mparams.use_mmap = true;
    
    model = llama_model_load_from_file(path, mparams);

    if (model == nullptr) {
        LOGE("NATIVE: Failed to load model");
        env->ReleaseStringUTFChars(model_path, path);
        return JNI_FALSE;
    }

    auto cparams = llama_context_default_params();
    cparams.n_ctx = 1024; 
    cparams.n_threads = 4; // Most mobile big cores are in groups of 2 or 4
    cparams.n_threads_batch = 4; // Crucial for faster prefill

    ctx = llama_init_from_model(model, cparams);

    if (ctx == nullptr) {
        LOGE("NATIVE: Failed to create context");
        llama_model_free(model);
        model = nullptr;
        env->ReleaseStringUTFChars(model_path, path);
        return JNI_FALSE;
    }

    LOGD("NATIVE: Model and Context initialized successfully");
    env->ReleaseStringUTFChars(model_path, path);
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_typeassist_app_service_MyAccessibilityService_stopGenerationNative(
        JNIEnv* env,
        jobject /* this */) {
    g_should_abort.store(true);
    LOGD("NATIVE: Stop signal received.");
}

static std::string trim(const std::string& s) {
    size_t first = s.find_first_not_of(" \t\n\r");
    if (std::string::npos == first) return "";
    size_t last = s.find_last_not_of(" \t\n\r");
    return s.substr(first, (last - first + 1));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_typeassist_app_service_MyAccessibilityService_generateResponseNative(
        JNIEnv* env,
        jobject /* this */,
        jstring prompt,
        jfloat temp,
        jfloat top_p,
        jint max_tokens) {

    std::lock_guard<std::mutex> lock(g_mutex);
    if (ctx == nullptr) return env->NewStringUTF("Error: Model not loaded");

    // If another thread is already generating, stop it first
    if (g_is_generating.load()) {
        LOGD("NATIVE: Previous generation detected. Signalling abort.");
        g_should_abort.store(true);
        // Busy wait a tiny bit for it to actually exit if needed, though usually the lock handles it.
    }

    g_is_generating.store(true);
    g_should_abort.store(false);
    
    // Register the abort callback with the context
    llama_set_abort_callback(ctx, abort_callback, nullptr);

    const char * prompt_text = env->GetStringUTFChars(prompt, nullptr);
    
    // First call to get the required number of tokens
    int n_tokens_needed = llama_tokenize(llama_model_get_vocab(model), prompt_text, strlen(prompt_text), nullptr, 0, true, true);
    if (n_tokens_needed < 0) {
        n_tokens_needed = abs(n_tokens_needed);
    }
    
    std::vector<llama_token> tokens_list(n_tokens_needed);
    const int n_tokens = llama_tokenize(llama_model_get_vocab(model), prompt_text, strlen(prompt_text), tokens_list.data(), tokens_list.size(), true, true);
    
    if (n_tokens < 0) {
        g_is_generating.store(false);
        env->ReleaseStringUTFChars(prompt, prompt_text);
        return env->NewStringUTF("Error: Tokenization failed");
    }

    // Always clear memory (KV cache) before fresh generation
    llama_memory_clear(llama_get_memory(ctx), true);

    struct llama_sampler * smpl_chain = llama_sampler_chain_init(llama_sampler_chain_default_params());
    
    // 1. Add Repetition Penalty (Critical for small models)
    // parameters: last_n, penalty_repeat, penalty_freq, penalty_present
    llama_sampler_chain_add(smpl_chain, llama_sampler_init_penalties(64, 1.1f, 0.0f, 0.0f));
    
    // 2. Standard samplers
    llama_sampler_chain_add(smpl_chain, llama_sampler_init_temp(temp));
    llama_sampler_chain_add(smpl_chain, llama_sampler_init_top_p(top_p, 1));
    llama_sampler_chain_add(smpl_chain, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    std::string response = "";
    llama_batch batch = llama_batch_get_one(tokens_list.data(), tokens_list.size());

    LOGD("NATIVE: Starting prefill for %d tokens...", n_tokens);
    int n_cur = 0;
    while (n_cur < max_tokens) {
        // Check for abort
        if (g_should_abort.load()) {
            LOGD("NATIVE: Abort detected in loop.");
            break;
        }

        if (llama_decode(ctx, batch)) {
            LOGE("NATIVE: Decode failed or aborted");
            break;
        }

        if (n_cur == 0) {
            LOGD("NATIVE: Prefill complete. Starting generation loop.");
        }

        llama_token id = llama_sampler_sample(smpl_chain, ctx, -1);
        llama_sampler_accept(smpl_chain, id);
        
        if (llama_vocab_is_eog(llama_model_get_vocab(model), id)) break;

        char buf[128];
        int n = llama_token_to_piece(llama_model_get_vocab(model), id, buf, sizeof(buf), 0, true);
        if (n > 0) {
            std::string piece = std::string(buf, n);
            response += piece;
            
            // STOP CHECK: Improved detection for ChatML and newlines
            if (response.find("<|im_end|>") != std::string::npos ||
                response.find("<|endoftext|>") != std::string::npos ||
                response.find("\n\n") != std::string::npos ||
                response.find("User:") != std::string::npos ||
                (response.find("Input:") != std::string::npos)) {
                LOGD("NATIVE: Stop sequence detected. Ending generation.");
                break;
            }
        }

        batch = llama_batch_get_one(&id, 1);
        n_cur++;
    }

    llama_sampler_free(smpl_chain);
    env->ReleaseStringUTFChars(prompt, prompt_text);
    
    // Clean up the response
    size_t stop_pos = response.find("<|");
    if (stop_pos != std::string::npos) response = response.substr(0, stop_pos);
    
    stop_pos = response.find("\n\n");
    if (stop_pos != std::string::npos) response = response.substr(0, stop_pos);

    g_is_generating.store(false);
    return env->NewStringUTF(trim(response).c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_typeassist_app_service_MyAccessibilityService_unloadModel(
        JNIEnv* env,
        jobject /* this */) {
    if (ctx) { llama_free(ctx); ctx = nullptr; }
    if (model) { llama_model_free(model); model = nullptr; }
    llama_backend_free();
    LOGD("NATIVE: Model unloaded");
}
