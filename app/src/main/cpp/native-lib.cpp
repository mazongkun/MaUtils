#include <jni.h>
#include <string>

#include "log_utils.h"
#include "opencv_utils.h"
#include "render.h"

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

JNIEXPORT void JNICALL
Java_com_mama_sample_lib_NativeLib_RGBA2Nv12(JNIEnv *env, jclass clazz,
                                             jbyteArray _rgba,
                                             jint width,
                                             jint height,
                                             jbyteArray _nv12) {
    if (_rgba == nullptr || _nv12 == nullptr || width <= 0 || height <= 0)
        return;

    jbyte * rgba = env->GetByteArrayElements(_rgba, nullptr);
    jbyte * nv12 = env->GetByteArrayElements(_nv12, nullptr);

    opencv_utils::RGBA2Nv12((unsigned char*) rgba, width, height, (unsigned char*) nv12);

    env->ReleaseByteArrayElements(_rgba, rgba, 0);
    env->ReleaseByteArrayElements(_nv12, nv12, 0);
}

// render
render * p_render = nullptr;
JNIEXPORT jint JNICALL
Java_com_mama_sample_lib_NativeLib_initRender(JNIEnv *env, jclass clazz,
                                              jint width,
                                              jint height) {
    if (width <= 0 || height <= 0) {
        return GL_PARAM_ERROR;
    }

    int ret = GL_OK;
    if (p_render == nullptr) {
        p_render = new render();
        ret = p_render->init(width, height);

        if (ret != GL_OK) {
            // error
            LOGE("init render error: %d", ret);
            p_render->destroy();
            delete p_render;
            p_render = nullptr;
        }
    }
    return ret;
}

JNIEXPORT jint JNICALL
Java_com_mama_sample_lib_NativeLib_renderToScreen(JNIEnv *env, jclass clazz,
                                                  jbyteArray _buffer ,
                                                  jint width,
                                                  jint height,
                                                  jboolean is_front_camera) {
    if (_buffer == nullptr || width <= 0 || height <= 0) {
        return BUFFER_ERROR;
    }

    jbyte  * buffer = env->GetByteArrayElements(_buffer, 0);
    int ret = GL_OK;

    ret = p_render->renderToScreen((unsigned char *) buffer, width, height, is_front_camera);
    LOGD("renderToScreen: %d", ret);

    env->ReleaseByteArrayElements(_buffer, buffer, 0);
    return ret;
}

JNIEXPORT jint JNICALL
Java_com_mama_sample_lib_NativeLib_renderTextureToScreen(JNIEnv *env, jclass clazz,
                                                         jint texture,
                                                         jint width,
                                                         jint height,
                                                         jboolean is_front_camera) {
    return p_render->renderTextureToScreen(texture, width, height, is_front_camera);
}

JNIEXPORT jint JNICALL
Java_com_mama_sample_lib_NativeLib_setLut(JNIEnv *env, jclass clazz,
                                          jbyteArray _buffer,
                                          jint width,
                                          jint height) {
    if (_buffer == nullptr || width <= 0 || height <= 0) {
        return BUFFER_ERROR;
    }
    jbyte * buffer = env->GetByteArrayElements(_buffer, 0);
    int ret = p_render->setLut((unsigned char*)buffer, width, height);
    env->ReleaseByteArrayElements(_buffer, buffer, 0);
    return ret;
}
JNIEXPORT jint JNICALL
Java_com_mama_sample_lib_NativeLib_renderLutToScreen(JNIEnv *env, jclass clazz,
                                                     jbyteArray _buffer,
                                                     jint width,
                                                     jint height,
                                                     jboolean is_front_camera) {
    if (_buffer == nullptr || width <= 0 || height <= 0) {
        return BUFFER_ERROR;
    }

    jbyte  * buffer = env->GetByteArrayElements(_buffer, 0);
    int ret = GL_OK;

    ret = p_render->renderLutToScreen((unsigned char *) buffer, width, height, is_front_camera);
    LOGD("renderLutToScreen: %d", ret);

    env->ReleaseByteArrayElements(_buffer, buffer, 0);
}

JNIEXPORT jint JNICALL
Java_com_mama_sample_lib_NativeLib_destroyRender(JNIEnv *env, jclass clazz) {
    if (p_render != nullptr) {
        p_render->destroy();
        delete p_render;
        p_render = nullptr;
    }
    return GL_OK;
}

} // extern "C"

