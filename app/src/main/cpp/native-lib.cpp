#include <jni.h>
#include <string>
#include "llama.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_typeassist_app_service_MyAccessibilityService_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string system_info = llama_print_system_info();
    std::string hello = "Hello from C++ (llama.cpp system info: " + system_info + ")";
    return env->NewStringUTF(hello.c_str());
}
