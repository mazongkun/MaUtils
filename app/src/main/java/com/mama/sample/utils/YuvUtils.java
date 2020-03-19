package com.mama.sample.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;

import com.mama.sample.lib.NativeLib;

import java.nio.ByteBuffer;

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

    public static byte[] RGBAToNv21(byte[] rgba, int width, int height) {
        if (rgba == null || width < 1 || height < 1) {
            return null;
        }

        byte[] nv21 = new byte[width * height * 3/2];
        NativeLib.RGBA2Nv21(rgba, width, height, nv21);
        return nv21;
    }

    public static byte[] RGBAToNv12(byte[] rgba, int width, int height) {
        if (rgba == null || width < 1 || height < 1) {
            return null;
        }

        byte[] nv12 = new byte[width * height * 3/2];
        NativeLib.RGBA2Nv12(rgba, width, height, nv12);
        return nv12;
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

    public static byte[] YUV_420_888toGRAY(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int ySize = width*height;

        byte[] y = new byte[ySize];

        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y

        int rowStride = image.getPlanes()[0].getRowStride();
        assert(image.getPlanes()[0].getPixelStride() == 1);

        int pos = 0;
        if (rowStride == width) { // likely
            yBuffer.get(y, 0, ySize);
            pos += ySize;
        } else {
            int yBufferPos = width - rowStride; // not an actual position
            for (; pos<ySize; pos+=width) {
                yBufferPos += rowStride - width;
                yBuffer.position(yBufferPos);
                yBuffer.get(y, pos, width);
            }
        }

        return y;
    }

    public static byte[] YUV_420_888toNV21(ByteBuffer yBuffer, ByteBuffer uBuffer, ByteBuffer vBuffer) {
        byte[] nv;

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv = new byte[ySize + uSize + vSize];

        yBuffer.get(nv, 0, ySize);
        vBuffer.get(nv, ySize, vSize);
        uBuffer.get(nv, ySize + vSize, uSize);
        return nv;
    }
}
