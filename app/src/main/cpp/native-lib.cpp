#include <jni.h>
#include <string>

#include "log_utils.h"
#include "opencv_utils.h"

extern "C" {
JNIEXPORT jstring JNICALL
Java_com_mama_sample_lib_NativeLib_stringFromJNI(
        JNIEnv *env,
        jclass /* this */) {
    std::string hello = "Hello from C++";
    LOGE2("Java_com_mama_sample_lib_NativeLib_stringFromJNI !");
    return env->NewStringUTF(hello.c_str());
}

JNIEXPORT void JNICALL
Java_com_mama_sample_lib_NativeLib_RGBA2Nv21(JNIEnv *env, jclass clazz,
                                             jbyteArray _rgba,
                                             jint width,
                                             jint height,
                                             jbyteArray _nv21) {
    if (_rgba == nullptr || _nv21 == nullptr || width <= 0 || height <= 0)
        return;

    jbyte * rgba = env->GetByteArrayElements(_rgba, nullptr);
    jbyte * nv21 = env->GetByteArrayElements(_nv21, nullptr);

    opencv_utils::RGBA2Nv21((unsigned char*) rgba, width, height, (unsigned char*) nv21);

    env->ReleaseByteArrayElements(_rgba, rgba, 0);
    env->ReleaseByteArrayElements(_nv21, nv21, 0);
}



} // extern "C"
