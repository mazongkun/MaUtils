package com.mama.sample.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;

public class YuvUtils {
    private static final String TAG = YuvUtils.class.getSimpleName();

    private static RenderScript rs = null;
    private static ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = null;

//    private static Type.Builder nv21Type, rgbaType;
//    private static Allocation in, out;

    private static RenderScript getRenderScript(Context context) {
        if (rs == null) {
            synchronized (YuvUtils.class) {
                if (rs == null) {
                    rs = RenderScript.create(context);
                }
            }
        }
        return rs;
    }

    private static ScriptIntrinsicYuvToRGB getYuvToRgbIntrinsic(Context context) {
        if (yuvToRgbIntrinsic == null) {
            synchronized (YuvUtils.class) {
                if (yuvToRgbIntrinsic == null) {
                    RenderScript renderScript = getRenderScript(context);
                    yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(renderScript, Element.U8_4(rs));
                }
            }
        }
        return yuvToRgbIntrinsic;
    }

    public static void init(Context context) {
        getRenderScript(context);
        getYuvToRgbIntrinsic(context);
    }

    public static void destroy() {
        rs.destroy();
        rs = null;
        yuvToRgbIntrinsic.destroy();
        yuvToRgbIntrinsic = null;
    }

    public static byte[] nv21ToRGBA(byte[] nv21, int width, int height){
        if (rs == null || yuvToRgbIntrinsic == null || nv21 == null || width < 1 || height < 1) {
            Log.e(TAG, "nv21ToRGBA params error: rs=" + rs
                    + ", yToR=" + yuvToRgbIntrinsic
                    + ", buf=" + nv21
                    + ", w=" + width + "x" + height);
            return null;
        }

        Type.Builder nv21Type, rgbaType;
        Allocation in, out;

        nv21Type = new Type.Builder(rs, Element.U8(rs)).setX(width).setY(height).setYuvFormat(ImageFormat.NV21);
        in = Allocation.createTyped(rs, nv21Type.create(), Allocation.USAGE_SCRIPT);

        rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
        out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);

        in.copyFrom(nv21);
        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);

        byte[] rgba = new byte[out.getBytesSize()];
        out.copyTo(rgba);

        in.destroy();
        out.destroy();
        return rgba;
    }

    public static Bitmap nv21ToBitmap(byte[] nv21, int width, int height){
        if (rs == null || yuvToRgbIntrinsic == null || nv21 == null || width < 1 || height < 1) {
            return null;
        }

        Type.Builder nv21Type, rgbaType;
        Allocation in, out;

        nv21Type = new Type.Builder(rs, Element.U8(rs)).setX(width).setY(height).setYuvFormat(ImageFormat.NV21);
        in = Allocation.createTyped(rs, nv21Type.create(), Allocation.USAGE_SCRIPT);

        rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
        out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);

        in.copyFrom(nv21);
        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);
//        Log.e(TAG, "mama= nv21ToRGBA byteSize=" + out.getBytesSize());

        Bitmap bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        out.copyTo(bmpout);
        return bmpout;
    }
}
