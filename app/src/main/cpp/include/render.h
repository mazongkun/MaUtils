//
// Created by mazongkun on 2020/3/9 0009.
//

#ifndef RENDER_H
#define RENDER_H
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include "opengl_utils.h"

#define LOOK_UP_TABLE_FUNC \
		"vec4 lookupTable(vec4 color, sampler2D lut){\n"\
		"    float blueColor = color.b * 63.0;\n"\
		"	 vec2 quad1;\n"\
		"    quad1.y = floor(floor(blueColor) / 8.0);\n"\
		"    quad1.x = floor(blueColor) - (quad1.y * 8.0);\n"\
		"    vec2 quad2;\n"\
		"    quad2.y = floor(ceil(blueColor) / 8.0);\n"\
		"    quad2.x = ceil(blueColor) - (quad2.y * 8.0);\n"\
		"    vec2 texPos1;\n"\
		"    texPos1.x = (quad1.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * color.r);\n"\
		"    texPos1.y = (quad1.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * color.g);\n"\
		"    vec2 texPos2;\n"\
		"    texPos2.x = (quad2.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * color.r);\n"\
		"    texPos2.y = (quad2.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * color.g);\n"\
		"    vec4 newColor1 = texture2D(lut, texPos1);\n"\
		"    vec4 newColor2 = texture2D(lut, texPos2);\n"\
		"    vec4 newColor = mix(newColor1, newColor2, fract(blueColor));\n"\
		"    //return newColor;\n"\
		"    return vec4(newColor.rgb, color.w);\n"\
		"    //return color;\n"\
		"}\n"

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
//LOOK_UP_TABLE_FUNC
const static char* FRAGMENT_SHADER_LUT = "" \
        "varying highp vec2 textureCoordinate;\n" \
		"uniform sampler2D inputImageTexture;\n" \
		"uniform sampler2D inputLutTexture;\n" \
		LOOK_UP_TABLE_FUNC
		"void main() {\n" \
		"     vec4 ori = texture2D(inputImageTexture, textureCoordinate); \n" \
		"     //vec4 lookup = lookupTable(ori, inputLutTexture);\n" \
		"     gl_FragColor = lookupTable(ori, inputLutTexture); \n" \
		"     //gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" \
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
    int mLutProgram;
    int mFramebuffer;
    int mTexture;
    int mLutTexture;

    GLfloat mVertexBuffer[8];
    GLfloat mTextureBuffer[8];
    GLfloat mMaskTextureBuffer[8];
    GLfloat mScreenTextureBuffer[8];

    int textureWidth;
    int textureHeight;
    int lutWidth;
    int lutHeight;

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
    int renderToScreen(unsigned char* buffer, int width, int height, bool is_front_camera);
    int renderTextureToScreen(int texture, int width, int height, bool is_front_camera);
    int setLut(unsigned char* buffer, int width, int height);
    int renderLutToScreen(unsigned char* buffer, int width, int height, bool is_front_camera);
    int destroy();

};


#endif //RENDER_H
