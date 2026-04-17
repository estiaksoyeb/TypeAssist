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
    std::string system_info = llama_print_system_info();
    std::string hello = "Hello from C++ (llama.cpp system info: " + system_info + ")";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_typeassist_app_service_MyAccessibilityService_loadModel(
        JNIEnv* env,
        jobject /* this */,
        jstring model_path) {
    
    if (model != nullptr) {
        LOGD("Model already loaded, unloading first...");
    }

    const char * path = env->GetStringUTFChars(model_path, nullptr);
    LOGD("Loading model from: %s", path);

    llama_backend_init();

    auto mparams = llama_model_default_params();
    model = llama_model_load_from_file(path, mparams);

    if (model == nullptr) {
        LOGE("Failed to load model from %s", path);
        env->ReleaseStringUTFChars(model_path, path);
        return JNI_FALSE;
    }

    auto cparams = llama_context_default_params();
    cparams.n_ctx = 2048; 
    cparams.n_threads = 4; 

    ctx = llama_init_from_model(model, cparams);

    if (ctx == nullptr) {
        LOGE("Failed to create context for model %s", path);
        llama_model_free(model);
        model = nullptr;
        env->ReleaseStringUTFChars(model_path, path);
        return JNI_FALSE;
    }

    LOGD("Model loaded successfully");
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

    if (ctx == nullptr) {
        return env->NewStringUTF("Error: Model not loaded");
    }

    const char * prompt_text = env->GetStringUTFChars(prompt, nullptr);
    
    // Tokenize the prompt
    std::vector<llama_token> tokens_list;
    tokens_list.resize(llama_n_ctx(ctx));
    const int n_tokens = llama_tokenize(llama_model_get_vocab(model), prompt_text, strlen(prompt_text), tokens_list.data(), tokens_list.size(), true, true);
    tokens_list.resize(n_tokens);

    // Simple inference loop (non-streaming for now)
    std::string response = "";
    
    // Prepare batch
    llama_batch batch = llama_batch_get_one(tokens_list.data(), tokens_list.size());

    // Initialize sampler
    auto sparams = llama_sampler_chain_default_params();
    struct llama_sampler * smpl = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(temp));
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(top_p, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(0));

    int n_cur = 0;
    while (n_cur < max_tokens) {
        if (llama_decode(ctx, batch)) {
            LOGE("Failed to decode");
            break;
        }

        const llama_token id = llama_sampler_sample(smpl, ctx, -1);
        
        if (llama_vocab_is_eog(llama_model_get_vocab(model), id)) {
            break;
        }

        char buf[256];
        int n = llama_token_to_piece(llama_model_get_vocab(model), id, buf, sizeof(buf), 0, true);
        if (n < 0) {
            LOGE("Failed to convert token to piece");
            break;
        }
        response += std::string(buf, n);

        llama_token token_id = id;
        batch = llama_batch_get_one(&token_id, 1);
        n_cur++;
    }

    llama_sampler_free(smpl);
    env->ReleaseStringUTFChars(prompt, prompt_text);
    return env->NewStringUTF(response.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_typeassist_app_service_MyAccessibilityService_unloadModel(
        JNIEnv* env,
        jobject /* this */) {
    if (ctx) {
        llama_free(ctx);
        ctx = nullptr;
    }
    if (model) {
        llama_model_free(model);
        model = nullptr;
    }
    llama_backend_free();
    LOGD("Model unloaded");
}
