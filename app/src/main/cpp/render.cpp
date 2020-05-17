//
// Created by mazongkun on 2020/3/9 0009.
//
#include "render.h"
#include "opengl_utils.h"
#include "log_utils.h"

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

int render::initProgram() {
    int ret = GL_OK;
    if ((ret = createProgram(VERTEX_SHADER, FRAGMENT_SHADER_2D, m2DProgram)) != GL_OK) {
        destroyProgram();
        return ret;
    }
    if ((ret = createProgram(VERTEX_SHADER, FRAGMENT_SHADER_LUT, mLutProgram)) != GL_OK) {
        destroyProgram();
        return ret;
    }
    isProgramInit = true;
    return GL_OK;
}
int render::destroyProgram() {
    LOGI("%s", __func__);
    glInternalDeleteProgram(m2DProgram);
    glInternalDeleteProgram(mLutProgram);
    m2DProgram = -1;
    mLutProgram = -1;
    isProgramInit = false;
    return GL_OK;
}

int render::initFramebuffer(int width, int height) {
    LOGI("%s", __func__);
    textureWidth  = width;
    textureHeight = height;

    int ret = GL_OK;

    glGenFramebuffers(1, (GLuint*)&mFramebuffer);
//    glGenTextures(1, (GLuint*)&mTexture);
//    LOGI("init mFramebuffer: %d, mTexture: %d", mFramebuffer, mTexture);
//
//    glBindTexture(GL_TEXTURE_2D, (GLuint)mTexture);
//    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);
//    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
//    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
//    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
//    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
//
//    glBindFramebuffer(GL_FRAMEBUFFER, (GLuint)mFramebuffer);
//    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, (GLuint)mTexture, 0);

    int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
    if (status != GL_FRAMEBUFFER_COMPLETE) {
        LOGE("bind framebuffer error, delete!");
        glInternalDeleteFramebuffers(1, &mFramebuffer);
//        glInternalDeleteTextures(1, &mTexture);
        return BUFFER_ERROR;
    }

    glBindTexture(GL_TEXTURE_2D, 0);
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    ret = glGetError();
    if (ret != GL_NO_ERROR) {
        LOGE("%s glerror=%d !", __func__, ret);
        destroyFramebuffer();
        return BUFFER_ERROR;
    }
    isFramebufferInit = true;
    return GL_OK;
}
int render::destroyFramebuffer() {
    LOGI("%s", __func__);
    glInternalDeleteFramebuffers(1, &mFramebuffer);
    int textures[] = {mTexture, mLutTexture};
    glInternalDeleteTextures(2, textures);
    isFramebufferInit = false;
    return GL_OK;
}

int render::init(int width, int height) {
    LOGI("%s width=%d, height=%d", __func__, width, height);
    if (width <= 0 || height <= 0) {
        return GL_PARAM_ERROR;
    }

    int ret = GL_OK;
    // program
    if (isProgramInit) {
        destroyProgram();
    }
    ret = initProgram();
    if (ret != GL_OK) {
        LOGE("initProgram error ret=%d !", ret);
        return ret;
    }
    // framebuffer
    if (isFramebufferInit) {
        destroyFramebuffer();
    }
    ret = initFramebuffer(width, height);
    if (ret != GL_OK) {
        LOGE("initFramebuffer error ret=%d !", ret);
        return ret;
    }

    frontCamera = true;

    memcpy(mVertexBuffer, VERTEX_POSITION, sizeof(VERTEX_POSITION));
    rotateTexture(mScreenTextureBuffer, 90, frontCamera, false);
    memcpy(mTextureBuffer, TEXTURE_POSITION, sizeof(TEXTURE_POSITION));
    memcpy(mMaskTextureBuffer, TEXTURE_POSITION, sizeof(TEXTURE_POSITION));
    //renderByMask
    rotateTexture(mMaskTextureBuffer, 0, false, true);

//    mMaskTextureRotation = -1;
//    mMaskTextureFlipHorizontal = false;
//    mMaskTextureFlipVertical = false;

//    mCustomRotateMask = false;

    isInit = true;
    return GL_OK;
}

int render::renderToScreen(unsigned char* buffer, int width, int height, bool is_front_camera) {
    LOGD("%s renderToScreen: %dx%d, %d, [%d, %d, %d]", __func__, width, height, is_front_camera, m2DProgram, mTexture, mFramebuffer);
    if (buffer == nullptr || width <= 0 || height <= 0) {
        return BUFFER_ERROR;
    }

    bool newSize = textureWidth != width || textureHeight != height;
    textureWidth  = width;
    textureHeight = height;

    mTexture = loadTexture(buffer, width, height, mTexture, newSize);
    LOGD("%s renderToScreen loadTexture: %d", __func__, mTexture);
    if (this->frontCamera != is_front_camera) {
        this->frontCamera = is_front_camera;
        rotateTexture(mScreenTextureBuffer, 90, is_front_camera, false);
    }
    glUseProgram((GLuint)m2DProgram);
    GLint vertexHandle  = glGetAttribLocation((GLuint)m2DProgram, "position");
    GLint textureHandle = glGetAttribLocation((GLuint)m2DProgram, "inputTextureCoordinate");
    LOGD("%s renderToScreen:handle: %d, %d", __func__, vertexHandle, textureHandle);
    glEnableVertexAttribArray((GLuint)vertexHandle);
    glVertexAttribPointer((GLuint)vertexHandle, COORDS_PER_VERTEX, GL_FLOAT, (GLboolean)false, VERTEX_STRIDE, mVertexBuffer);
    glEnableVertexAttribArray((GLuint)textureHandle);
    glVertexAttribPointer((GLuint)textureHandle, COORDS_PER_VERTEX, GL_FLOAT, (GLboolean)false, VERTEX_STRIDE, mScreenTextureBuffer);

    glBindFramebuffer(GL_FRAMEBUFFER, 0);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, (GLuint)mTexture);
    glUniform1i(glGetUniformLocation((GLuint)m2DProgram, "inputImageTexture"), 0);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    glDisableVertexAttribArray((GLuint)vertexHandle);
    glDisableVertexAttribArray((GLuint)textureHandle);

    int ret = glGetError();
    if (ret != GL_NO_ERROR) {
        LOGE("%s glerror=%d !", __func__, ret);
        return BUFFER_ERROR;
    }
    return GL_OK;
}

int render::renderTextureToScreen(int texture, int width, int height, bool is_front_camera) {
    if (texture <0 || width <= 0 || height <= 0) {
        return BUFFER_ERROR;
    }

    if (this->frontCamera != is_front_camera) {
        this->frontCamera = is_front_camera;
        rotateTexture(mScreenTextureBuffer, 90, is_front_camera, false);
    }
    glUseProgram((GLuint)m2DProgram);
    GLint vertexHandle  = glGetAttribLocation((GLuint)m2DProgram, "position");
    GLint textureHandle = glGetAttribLocation((GLuint)m2DProgram, "inputTextureCoordinate");
    glEnableVertexAttribArray((GLuint)vertexHandle);
    glVertexAttribPointer((GLuint)vertexHandle, COORDS_PER_VERTEX, GL_FLOAT, (GLboolean)false, VERTEX_STRIDE, mVertexBuffer);
    glEnableVertexAttribArray((GLuint)textureHandle);
    glVertexAttribPointer((GLuint)textureHandle, COORDS_PER_VERTEX, GL_FLOAT, (GLboolean)false, VERTEX_STRIDE, mScreenTextureBuffer);

    glBindFramebuffer(GL_FRAMEBUFFER, 0);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, (GLuint)texture);
    glUniform1i(glGetUniformLocation((GLuint)m2DProgram, "inputImageTexture"), 0);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    glDisableVertexAttribArray((GLuint)vertexHandle);
    glDisableVertexAttribArray((GLuint)textureHandle);

    int ret = glGetError();
    if (ret != GL_NO_ERROR) {
        LOGE("%s glerror=%d !", __func__, ret);
        return BUFFER_ERROR;
    }
    return GL_OK;
}

int render::setLut(unsigned char *buffer, int width, int height) {
    LOGD("%s setLut: %dx%d", __func__, width, height);
    if (buffer == nullptr || width <= 0 || height <= 0) {
        return BUFFER_ERROR;
    }
    bool newSize = lutWidth != width || lutHeight != height;
    lutWidth  = width;
    lutHeight = height;
    mLutTexture = loadTexture(buffer, width, height, mLutTexture, newSize);
    return GL_OK;
}

int render::renderLutToScreen(unsigned char* buffer, int width, int height, bool is_front_camera) {
    LOGD("%s renderLutToScreen: %dx%d, %d, [%d, %d, %d, %d]", __func__, width, height, is_front_camera, mLutProgram, mTexture, mLutTexture, mFramebuffer);
    if (buffer == nullptr || width <= 0 || height <= 0) {
        return BUFFER_ERROR;
    }

    bool newSize = textureWidth != width || textureHeight != height;
    textureWidth  = width;
    textureHeight = height;
    mTexture = loadTexture(buffer, width, height, mTexture, newSize);
    LOGD("%s renderLutToScreen: %dx%d, %d, [%d, %d, %d, %d]", __func__, width, height, is_front_camera, mLutProgram, mTexture, mLutTexture, mFramebuffer);

    LOGD("%s renderToScreen loadTexture: %d", __func__, mTexture);
    if (this->frontCamera != is_front_camera) {
        this->frontCamera = is_front_camera;
        rotateTexture(mScreenTextureBuffer, 90, is_front_camera, false);
    }
    glUseProgram((GLuint)mLutProgram);
    GLint vertexHandle  = glGetAttribLocation((GLuint)mLutProgram, "position");
    GLint textureHandle = glGetAttribLocation((GLuint)mLutProgram, "inputTextureCoordinate");
    GLint inputImageHandle = glGetUniformLocation((GLuint)mLutProgram, "inputImageTexture");
    GLint inputLutHandle = glGetUniformLocation((GLuint)mLutProgram, "inputLutTexture");
    LOGD("%s renderLutToScreen:handle: %d, %d, %d, %d", __func__, vertexHandle, textureHandle, inputImageHandle, inputLutHandle);
    glEnableVertexAttribArray((GLuint)vertexHandle);
    glVertexAttribPointer((GLuint)vertexHandle, COORDS_PER_VERTEX, GL_FLOAT, (GLboolean)false, VERTEX_STRIDE, mVertexBuffer);
    glEnableVertexAttribArray((GLuint)textureHandle);
    glVertexAttribPointer((GLuint)textureHandle, COORDS_PER_VERTEX, GL_FLOAT, (GLboolean)false, VERTEX_STRIDE, mScreenTextureBuffer);

//    glBindFramebuffer(GL_FRAMEBUFFER, 0);
//    glViewport(0, 0, mTextureWidth, mTextureHeight);
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
//    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, mTexture, 0);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, (GLuint)mTexture);
    glUniform1i(inputImageHandle, 0);

    glActiveTexture(GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D, (GLuint)mLutTexture);
    glUniform1i(inputLutHandle, 1);

    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    glDisableVertexAttribArray((GLuint)vertexHandle);
    glDisableVertexAttribArray((GLuint)textureHandle);

    int ret = glGetError();
    if (ret != GL_NO_ERROR) {
        LOGE("%s glerror=%d !", __func__, ret);
        return BUFFER_ERROR;
    }
    return GL_OK;
}

int render::destroy() {
    LOGI("%s", __func__);

    if (isProgramInit) {
        destroyProgram();
    }
    if (isFramebufferInit) {
        destroyFramebuffer();
    }

    isInit = false;
    return GL_OK;
}

render::render() : m2DProgram(-1), mLutProgram(-1), mFramebuffer(-1), mTexture(-1), mLutTexture(-1),
                   mVertexBuffer(), mTextureBuffer(),
                   mMaskTextureBuffer(),
                   mScreenTextureBuffer(),
                   textureWidth(0), textureHeight(0), lutWidth(0), lutHeight(0),
                   isInit(false), isProgramInit(false), isFramebufferInit(false) {}


