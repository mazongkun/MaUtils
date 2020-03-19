package com.mama.sample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.mama.sample.base.BaseActivity;
import com.mama.sample.lib.NativeLib;
import com.mama.sample.utils.BitmapUtil;
import com.mama.sample.utils.FileUtils;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        checkPermission(null);
    }

    private void initView() {
        TextView tv = findViewById(R.id.sample_text);
        tv.setText(NativeLib.stringFromJNI());

        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission(null)) {
                    processImage();
                } else {
                    showToast("请打开权限");
                }
            }
        });
    }

    private void processImage() {
        String imagePath = FileUtils.MA_PATH + "/jt.jpg";

        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        if (bitmap != null) {
            Log.d(TAG, "bitmap: w=" + bitmap.getWidth() + ", h=" + bitmap.getHeight());

            int width  = bitmap.getWidth();
            int height = bitmap.getHeight();
            byte[] rgba = BitmapUtil.getRGBAFromBitmap(bitmap);

            byte[] nv21 = new byte[width * height * 3/2];
            byte[] nv12 = new byte[width * height * 3/2];

            NativeLib.RGBA2Nv21(rgba, width, height, nv21);
            NativeLib.RGBA2Nv12(rgba, width, height, nv12);
            FileUtils.saveFile(FileUtils.MA_PATH + "/jt_" + width + "x" + height + "_nv21.yuv", nv21);
            FileUtils.saveFile(FileUtils.MA_PATH + "/jt_" + width + "x" + height + "_nv12.yuv", nv12);
        } else {
            Log.e(TAG, "bitmap = " + bitmap);
        }
    }
}
