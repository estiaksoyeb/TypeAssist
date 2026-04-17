#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_typeassist_app_service_MyAccessibilityService_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++ (TypeAssist)";
    return env->NewStringUTF(hello.c_str());
}
