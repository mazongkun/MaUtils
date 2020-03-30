//
// Created by Administrator on 2020/3/8 0008.
//

#ifndef OPENGL_UTILS_H
#define OPENGL_UTILS_H

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#define GL_OK          (0)	    ///< 正常运行
#define GL_PARAM_ERROR (-1001)	///< 参数错误
#define SHADER_ERROR   (-1002)	///< Shader错误
#define PROGRAM_ERROR  (-1003)	///< Program错误
#define BUFFER_ERROR   (-1004)	///< Buffer错误
#define TEXTURE_ERROR  (-1005)	///< 纹理错误
#define INTERNAL_ERROR (-1006)	///< 内部错误

const GLfloat TEXTURE_NO_ROTATION[] = {
        0.0f, 1.0f,
        1.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 0.0f,
};

const GLfloat TEXTURE_ROTATED_90[] = {
        1.0f, 1.0f,
        1.0f, 0.0f,
        0.0f, 1.0f,
        0.0f, 0.0f,
};
const GLfloat TEXTURE_ROTATED_180[] = {
        1.0f, 0.0f,
        0.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 1.0f,
};
const GLfloat TEXTURE_ROTATED_270[] = {
        0.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 0.0f,
        1.0f, 1.0f,
};


const GLfloat flip(const GLfloat i) ;
const void rotateTexture(GLfloat* texture, const int rotation, const bool flipHorizontal, const bool flipVertical) ;
void printGLString(const char *name, GLenum s);
void checkGlError(const char* op);
int createProgram(const char* vertexSource, const char* fragmentSource, int & programId);
GLuint loadSingleChannelTexture(const unsigned char* data, const int width, const int height, const int usedTexId);
GLuint loadTexture(const unsigned char* data, const int width, const int height, const int usedTexId, bool newSize);

void glInternalDeleteProgram(int & program);
void glInternalDeleteFramebuffers(int num, int* buffers);
void glInternalDeleteTextures(int num, int* textures);

void dumpTexture(int textureId, int width, int height, const char* fileName);

#endif //OPENGL_UTILS_H
