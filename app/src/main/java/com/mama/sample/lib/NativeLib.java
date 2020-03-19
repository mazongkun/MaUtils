package com.mama.sample.lib;

public class NativeLib {

    public native static String stringFromJNI();
    public native static void RGBA2Nv21(byte[] rgba, int width, int height, byte[] nv21);
    public native static void RGBA2Nv12(byte[] rgba, int width, int height, byte[] nv12);

    static {
        System.loadLibrary("native-lib");
    }
}