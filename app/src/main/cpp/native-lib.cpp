#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "llama.h"

#define TAG "TypeAssistNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static llama_model * model = nullptr;
static llama_context * ctx = nullptr;

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

    auto mparams = llama_model_default_params();
    // Try to use memory mapping for faster loading
    mparams.use_mmap = true;
    
    model = llama_model_load_from_file(path, mparams);

    if (model == nullptr) {
        LOGE("NATIVE: Failed to load model");
        env->ReleaseStringUTFChars(model_path, path);
        return JNI_FALSE;
    }

    auto cparams = llama_context_default_params();
    cparams.n_ctx = 1024; // Smaller context for stability
    cparams.n_threads = 4; 

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

extern "C" JNIEXPORT jstring JNICALL
Java_com_typeassist_app_service_MyAccessibilityService_generateResponseNative(
        JNIEnv* env,
        jobject /* this */,
        jstring prompt,
        jfloat temp,
        jfloat top_p,
        jint max_tokens) {

    if (ctx == nullptr) return env->NewStringUTF("Error: Model not loaded");

    const char * prompt_text = env->GetStringUTFChars(prompt, nullptr);
    LOGD("NATIVE: Tokenizing prompt...");
    
    std::vector<llama_token> tokens_list;
    tokens_list.resize(llama_n_ctx(ctx));
    const int n_tokens = llama_tokenize(llama_model_get_vocab(model), prompt_text, strlen(prompt_text), tokens_list.data(), tokens_list.size(), true, true);
    if (n_tokens < 0) {
        env->ReleaseStringUTFChars(prompt, prompt_text);
        return env->NewStringUTF("Error: Tokenization failed");
    }
    tokens_list.resize(n_tokens);
    LOGD("NATIVE: %d tokens to process", n_tokens);

    // Ultra-stable sampler setup
    struct llama_sampler * smpl = llama_sampler_init_temp(temp);
    // Note: older versions used different top_p init, we stick to basic temp if needed, 
    // but let's try to add top_p safely
    struct llama_sampler * smpl_chain = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(smpl_chain, smpl);
    llama_sampler_chain_add(smpl_chain, llama_sampler_init_top_p(top_p, 1));
    llama_sampler_chain_add(smpl_chain, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    std::string response = "";
    llama_batch batch = llama_batch_get_one(tokens_list.data(), tokens_list.size());

    int n_cur = 0;
    LOGD("NATIVE: Starting decode loop...");
    while (n_cur < max_tokens) {
        LOGD("NATIVE: Decoding token %d...", n_cur);
        int decode_res = llama_decode(ctx, batch);
        if (decode_res != 0) {
            LOGE("NATIVE: Decode failed with code %d", decode_res);
            break;
        }

        llama_token id = llama_sampler_sample(smpl_chain, ctx, -1);
        llama_sampler_accept(smpl_chain, id);
        
        if (llama_vocab_is_eog(llama_model_get_vocab(model), id)) {
            LOGD("NATIVE: EOG reached");
            break;
        }

        char buf[128];
        int n = llama_token_to_piece(llama_model_get_vocab(model), id, buf, sizeof(buf), 0, true);
        if (n > 0) {
            response += std::string(buf, n);
            LOGD("NATIVE: Generated piece: %s", std::string(buf, n).c_str());
        }

        batch = llama_batch_get_one(&id, 1);
        n_cur++;
    }

    LOGD("NATIVE: Inference finished. Response: %s", response.c_str());

    llama_sampler_free(smpl_chain);
    env->ReleaseStringUTFChars(prompt, prompt_text);
    return env->NewStringUTF(response.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_typeassist_app_service_MyAccessibilityService_unloadModel(
        JNIEnv* env,
        jobject /* this */) {
    if (ctx) { llama_free(ctx); ctx = nullptr; }
    if (model) { llama_model_free(model); model = nullptr; }
    llama_backend_free();
    LOGD("NATIVE: Cleanup complete");
}
