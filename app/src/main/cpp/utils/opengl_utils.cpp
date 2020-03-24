//
// Created by mazongkun on 2020/3/8 0008.
//

#include "opengl_utils.h"
#include "log_utils.h"

#include <stdlib.h>
#include <string.h>
const GLfloat flip(const GLfloat i) {
    if (i == 0.0f) {
        return 1.0f;
    }
    return 0.0f;
}
const void rotateTexture(GLfloat* texture, const int rotation, const bool flipHorizontal, const bool flipVertical) {
    switch (rotation) {
        case 90:
            memcpy(texture, TEXTURE_ROTATED_90, sizeof(TEXTURE_ROTATED_90));
            break;
        case 180:
            memcpy(texture, TEXTURE_ROTATED_180, sizeof(TEXTURE_ROTATED_180));
            break;
        case 270:
            memcpy(texture, TEXTURE_ROTATED_270, sizeof(TEXTURE_ROTATED_270));
            break;
        case 0:
        default:
            memcpy(texture, TEXTURE_NO_ROTATION, sizeof(TEXTURE_NO_ROTATION));
            break;
    }
    if (flipHorizontal) {
        texture[0] = flip(texture[0]);
        texture[2] = flip(texture[2]);
        texture[4] = flip(texture[4]);
        texture[6] = flip(texture[6]);
    }
    if (flipVertical) {
        texture[1] = flip(texture[1]);
        texture[3] = flip(texture[3]);
        texture[5] = flip(texture[5]);
        texture[7] = flip(texture[7]);
    }
}

void printGLString(const char *name, GLenum s) {
    const char *v = (const char *) glGetString(s);
    LOGI("GL %s = %s\n", name, v);
}

void checkGlError(const char* op) {
    for (GLint error = glGetError(); error; error
                                                    = glGetError()) {
        LOGE("after %s() glError (0x%x)\n", op, error);
    }
}


GLuint loadShader(GLenum type, const char* source) {
    GLuint shader = glCreateShader(type);
    if (shader) {
        glShaderSource(shader, 1, &source, NULL);
        glCompileShader(shader);
        GLint compiled = 0;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
        if (!compiled) {
            GLint infoLen = 0;
            glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
            if (infoLen) {
                char* buf = (char*) malloc(infoLen);
                if (buf) {
                    glGetShaderInfoLog(shader, infoLen, NULL, buf);
                    LOGE("Could not compile shader %d:\n%s\n",
                         type, buf);
                    free(buf);
                }
                glDeleteShader(shader);
                shader = 0;
            }
        }
    }
    return shader;
}

int createProgram(const char* vertexSource, const char* fragmentSource, int & programId) {
    GLuint vertexShader = loadShader(GL_VERTEX_SHADER, vertexSource);
    if (!vertexShader) {
        return SHADER_ERROR;
    }

    GLuint pixelShader = loadShader(GL_FRAGMENT_SHADER, fragmentSource);
    if (!pixelShader) {
        return SHADER_ERROR;
    }

    GLuint program = glCreateProgram();
    if (program) {
        glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        glAttachShader(program, pixelShader);
        checkGlError("glAttachShader");
        glLinkProgram(program);
        GLint linkStatus = GL_FALSE;
        glGetProgramiv(program, GL_LINK_STATUS, &linkStatus);
        glDeleteShader(vertexShader);
        glDeleteShader(pixelShader);
        if (linkStatus != GL_TRUE) {
            GLint bufLength = 0;
            glGetProgramiv(program, GL_INFO_LOG_LENGTH, &bufLength);
            if (bufLength) {
                char* buf = (char*) malloc(bufLength);
                if (buf) {
                    glGetProgramInfoLog(program, bufLength, NULL, buf);
                    LOGE("Could not link program:\n%s\n", buf);
                    free(buf);
                }
            }
            glDeleteProgram(program);
            return PROGRAM_ERROR;
        }
    }
    if (!glIsProgram(program)) {
        LOGE("createProgram %d is not program !!\n", program);
        return PROGRAM_ERROR;
    }
    programId = program;
    return GL_OK;
}
GLuint loadSingleChannelTexture(const unsigned char* data, const int width, const int height, const int usedTexId, bool newSize) {
    GLuint textures[1];
    if (usedTexId == -1) {
        glGenTextures(1, textures);
        glBindTexture(GL_TEXTURE_2D, textures[0]);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, width, height, 0, GL_ALPHA, GL_UNSIGNED_BYTE, data);
    } else {
        glBindTexture(GL_TEXTURE_2D, (GLuint)usedTexId);
        if (newSize) {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, width, height, 0, GL_ALPHA, GL_UNSIGNED_BYTE, data);
        } else {
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_ALPHA, GL_UNSIGNED_BYTE, data);
        }
        textures[0] = (GLuint)usedTexId;
    }
    return textures[0];
}
GLuint loadTexture(const unsigned char* data, const int width, const int height, const int usedTexId, bool newSize) {
    GLuint textures[1];
    LOGD("loadTexture: usedTexId = %d", usedTexId);
    if (usedTexId == -1) {
        glGenTextures(1, textures);
        glBindTexture(GL_TEXTURE_2D, textures[0]);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
    } else {
        glBindTexture(GL_TEXTURE_2D, (GLuint)usedTexId);
        if (newSize) {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
        } else {
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, data);
        }
        textures[0] = (GLuint) usedTexId;
    }
    return textures[0];
}
void glInternalDeleteProgram(int & program) {
    glDeleteProgram((GLuint)program);
    program = -1;
}
void glInternalDeleteFramebuffers(int num, int* buffers) {
    for(int i=0; i<num; i++) {
        if (((int)buffers[i]) > 0) {
            glDeleteFramebuffers(1, (GLuint*)&buffers[i]);
        }
        buffers[i] = -1;
    }
}
void glInternalDeleteTextures(int num, int* textures) {
    for(int i=0; i<num; i++) {
        if ((textures[i]) > 0) {
            glDeleteTextures(1, (GLuint*)&textures[i]);
        }
        textures[i] = -1;
    }
}