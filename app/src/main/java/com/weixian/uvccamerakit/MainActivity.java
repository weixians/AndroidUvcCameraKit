package com.weixian.uvccamerakit;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.serenegiant.widget.CameraViewInterface;
import com.serenegiant.GGCamera;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private CameraViewInterface mUVCCameraView;
    private ImageView ivCrop;
    private ImageView ivBinary;
    private ImageView ivCircle;

    private AppCompatActivity activity;
    private GGCamera ggCamera;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;

        initView();
        checkPermission();

        ggCamera = GGCamera.getInstance();
        ggCamera.onCreate(this, mUVCCameraView, buildCameraDataDecodingCallback());
    }

    private void initView() {
        mUVCCameraView = findViewById(R.id.camera_view);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ggCamera.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        ggCamera.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ggCamera.onDestroy();
    }


    private GGCamera.DecodingCallback buildCameraDataDecodingCallback() {
        return new GGCamera.DecodingCallback() {
            @Override
            public void onDataReceived(Bitmap bitmap) {
                processImage(bitmap);
            }
        };
    }

    private void processImage(Bitmap bitmap) {
        // 图像处理操作
    }


    private void checkPermission() {
        XXPermissions.with(this)
                // 申请单个权限
                .permission(Permission.CAMERA)
                .request(new OnPermissionCallback() {

                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        Toast.makeText(activity, "相机权限获取成功", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        if (never) {
                            // 如果是被永久拒绝就跳转到应用权限系统设置页面
                            XXPermissions.startPermissionActivity(getBaseContext(), permissions);
                        } else {
                            checkPermission();
                        }
                    }
                });
    }
}
