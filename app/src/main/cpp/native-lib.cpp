#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_mama_sample_lib_NativeLib_stringFromJNI(
        JNIEnv *env,
        jclass /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
