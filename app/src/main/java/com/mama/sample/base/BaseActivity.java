package com.mama.sample.base;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class BaseActivity extends AppCompatActivity {
    protected final String TAG = getClass().getSimpleName();

    private static final int REQUEST_PERMISSION = 1;
    private final String[] PERMISSIONS = new String[] {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    protected OnPermissionListener permissionListener = null;

    public interface OnPermissionListener {
        void onResult(boolean success);
    }

    protected void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    protected boolean checkPermission(OnPermissionListener permissionListener) {
        this.permissionListener = permissionListener;
        for (int i = 0; i < PERMISSIONS.length; i++) {
            int state = ContextCompat.checkSelfPermission(this, PERMISSIONS[i]);
            if (state != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults[0] != PERMISSION_GRANTED) {
                Log.e(TAG, "onRequestPermissionsResult f");
//                Toast.makeText(this, "请打开权限", Toast.LENGTH_SHORT).show();
//                Intent intent = new Intent();
//                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                Uri uri = Uri.fromParts("package", getPackageName(), null);
//                intent.setData(uri);
//                startActivityForResult(intent, REQUEST_PERMISSION);
                if (permissionListener != null) {
                    permissionListener.onResult(false);
                }
            } else {
                Log.e(TAG, "onRequestPermissionsResult e");
                if (permissionListener != null) {
                    permissionListener.onResult(true);
                }
            }
        }
    }
}
