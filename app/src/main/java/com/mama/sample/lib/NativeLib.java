package com.mama.sample.lib;

public class NativeLib {
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native static String stringFromJNI();

    static {
        System.loadLibrary("native-lib");
    }
}