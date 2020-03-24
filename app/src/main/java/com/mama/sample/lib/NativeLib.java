package com.mama.sample.lib;

public class NativeLib {

    public native static String stringFromJNI();
    // format convert
    public native static void RGBA2Nv21(byte[] rgba, int width, int height, byte[] nv21);
    public native static void RGBA2Nv12(byte[] rgba, int width, int height, byte[] nv12);

    // render
    public static native int initRender(int width, int height);
    public static native int renderToScreen(byte[] buffer, int width, int height, boolean isFrontCamera);
    public static native int renderTextureToScreen(int texture, int width, int height, boolean isFrontCamera);
    public static native int destroyRender();

    static {
        System.loadLibrary("native-lib");
    }
}