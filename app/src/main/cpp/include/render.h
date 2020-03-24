//
// Created by mazongkun on 2020/3/9 0009.
//

#ifndef RENDER_H
#define RENDER_H
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include "opengl_utils.h"

const static char* VERTEX_SHADER = "" \
		"attribute vec4 position;\n" \
		"attribute vec4 inputTextureCoordinate;\n" \
		"varying vec2 textureCoordinate;\n" \
		"void main() {\n" \
		"    gl_Position = position;\n" \
		"    textureCoordinate = inputTextureCoordinate.xy;\n" \
		"}";

const static char* FRAGMENT_SHADER_2D = "" \
		"varying highp vec2 textureCoordinate;\n" \
		"uniform sampler2D inputImageTexture;\n" \
		"void main() {\n" \
		"     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" \
		"}";

const static GLfloat VERTEX_POSITION[] = {
		-1, -1,
		1, -1,
		-1,  1,
		1,  1,
};
const static GLfloat TEXTURE_POSITION[] = {
		0, 0,
		1, 0,
		0, 1,
		1, 1,
};

const static int COORDS_PER_VERTEX = 2;
const static int VERTEX_STRIDE = COORDS_PER_VERTEX * 4;

class render {

private:
    int m2DProgram;
    int mFramebuffer;
    int mTexture;

    GLfloat mVertexBuffer[8];
    GLfloat mTextureBuffer[8];
    GLfloat mMaskTextureBuffer[8];
    GLfloat mScreenTextureBuffer[8];

    int textureWidth;
    int textureHeight;

    bool frontCamera;

    bool isInit 		   = false;
	bool isProgramInit     = false;
	bool isFramebufferInit = false;

	int initProgram();
	int destroyProgram();

	int initFramebuffer(int width, int height);
	int destroyFramebuffer();
public:
    render();

    int init(int width, int height);
    int renderToScreen(unsigned char* buffer, int width, int height, bool isFrontCamera);
    int renderTextureToScreen(int texture, int width, int height, bool isFrontCamera);
    int destroy();

};


#endif //RENDER_H
