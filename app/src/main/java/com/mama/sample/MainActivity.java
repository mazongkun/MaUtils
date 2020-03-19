package com.mama.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.mama.sample.base.BaseActivity;
import com.mama.sample.lib.NativeLib;
import com.mama.sample.utils.BitmapUtil;
import com.mama.sample.utils.YuvUtils;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        boolean success = checkPermission(new OnPermissionListener() {
            @Override
            public void onResult(boolean success) {
                if (success) {

                }
            }
        });
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
        String imagePath = Environment.getExternalStorageDirectory() + "/sensetime/jt.jpg";

        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        if (bitmap != null) {
            Log.d(TAG, "bitmap: w=" + bitmap.getWidth() + ", h=" + bitmap.getHeight());

            int width  = bitmap.getWidth();
            int height = bitmap.getHeight();
            byte[] rgba = BitmapUtil.getRGBAFromBitmap(bitmap);
//            byte[] nv21 = YuvUtils.RGBAToNv21(rgba, width, height);

//            Bitmap grayBitmap = BitmapUtil.getBitmapFromGrayBuffer(nv21, width, height);
//            BitmapUtil.saveBitmap(Environment.getExternalStorageDirectory() + "/sensetime/jt_gray.jpg", grayBitmap);
        } else {
            Log.e(TAG, "bitmap = " + bitmap);
        }
    }
}
