#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "LlamaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jlong JNICALL
Java_com_createexe_exe_llm_LlamaEngine_nativeLoadModel(JNIEnv *env, jobject thiz, jstring model_path) {
    const char *path = env->GetStringUTFChars(model_path, nullptr);
    LOGI("Loading GGUF model from: %s", path);
    // TODO: Initialize llama_context here once llama.cpp source is dropped in
    env->ReleaseStringUTFChars(model_path, path);
    
    // Return a dummy memory pointer for now
    return 123456789LL;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_createexe_exe_llm_LlamaEngine_nativeGenerate(JNIEnv *env, jobject thiz, jlong context_ptr, jstring prompt) {
    const char *prompt_cstr = env->GetStringUTFChars(prompt, nullptr);
    LOGI("Received prompt: %s", prompt_cstr);
    
    // TODO: Run llama_decode and sample here
    std::string response = "Native Engine Acknowledged: Processing via Nullclaw.";
    
    env->ReleaseStringUTFChars(prompt, prompt_cstr);
    return env->NewStringUTF(response.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_createexe_exe_llm_LlamaEngine_nativeRelease(JNIEnv *env, jobject thiz, jlong context_ptr) {
    LOGI("Releasing native model context.");
    // TODO: llama_free(ctx)
}
