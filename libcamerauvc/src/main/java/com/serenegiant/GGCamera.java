package com.serenegiant;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.widget.CameraViewInterface;

import java.util.Timer;
import java.util.TimerTask;

public class GGCamera {
    public interface DecodingCallback {
        void onDataReceived(Bitmap bitmap);
    }

    /**
     * preview resolution(width)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    public static int PREVIEW_WIDTH = 640;
    /**
     * preview resolution(height)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    public static int PREVIEW_HEIGHT = 360;
    /**
     * set true if you want to record movie using MediaSurfaceEncoder
     * (writing frame data into Surface camera from MediaCodec
     * by almost same way as USBCameratest2)
     * set false if you want to record movie using MediaVideoEncoder
     */
    private static final boolean USE_SURFACE_ENCODER = true;
    /**
     * preview mode
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     * 0:YUYV, other:MJPEG
     */
    private static final int PREVIEW_MODE = 0; // YUV
    private UVCCameraHandler mCameraHandler;
    private UVCCamera uvcCamera;

    private CameraViewInterface mUVCCameraView;
    private USBMonitor mUSBMonitor;
    private Timer timer = null;
    private Activity activity;

    private static GGCamera instance;

    private GGCamera() {
    }

    public static GGCamera getInstance() {
        if (instance == null) {
            instance = new GGCamera();
        }
        return instance;
    }

    /**
     * 该对象可用于设置或者获取一些设备的参数，比如：
     * 1. 设备支持的预览比例 uvcCamera.getSupportedSize()
     * 2. 白平衡
     * 3. 亮度
     * 等等
     *
     * @return
     */
    public UVCCamera getUVCCamera() {
        return uvcCamera;
    }

    public void onCreate(Activity activity, CameraViewInterface cameraView, DecodingCallback callback) {
        this.activity = activity;
        mUVCCameraView = cameraView;
        mUVCCameraView.setAspectRatio(PREVIEW_WIDTH / (double) PREVIEW_HEIGHT);
        mCameraHandler = UVCCameraHandler.createHandler(activity, mUVCCameraView,
                USE_SURFACE_ENCODER ? 0 : 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE);
        uvcCamera = new UVCCamera();

        USBMonitor.OnDeviceConnectListener listener = setupListener(callback);
        mUSBMonitor = new USBMonitor(activity, listener);
    }

    public void onStart() {
        if (mUSBMonitor != null) {
            mUSBMonitor.register();
        }
    }

    public void onStop() {
        if (mUSBMonitor != null) {
            mUSBMonitor.unregister();
        }
    }

    public void onDestroy() {
        if (mCameraHandler != null) {
            mCameraHandler.close();
        }
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
    }

    private void getUsbDev() {
        if (mUSBMonitor != null) {
            UsbDevice usbDevice = mUSBMonitor.getDeviceList().get(0);
            mUSBMonitor.requestPermission(usbDevice);
        }
    }

    private USBMonitor.OnDeviceConnectListener setupListener(DecodingCallback callback) {
        return new USBMonitor.OnDeviceConnectListener() {
            @Override
            public void onAttach(final UsbDevice device) {
                Toast.makeText(activity, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
                getUsbDev();

            }

            @Override
            public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
                Log.i("WebCamTest", "onConnect ");
                if (mCameraHandler != null) {
                    mCameraHandler.open(ctrlBlock);
                    startPreview();
                    startDecoding(callback);
                }
            }

            @Override
            public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
                stopDecoding();
            }

            @Override
            public void onDetach(final UsbDevice device) {
                Toast.makeText(activity, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
                mCameraHandler.close();
            }

            @Override
            public void onCancel(final UsbDevice device) {
            }
        };
    }

    private void startPreview() {
        if (mCameraHandler != null) {
            final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
            mCameraHandler.startPreview(new Surface(st));
        }
    }


    private void startDecoding(DecodingCallback callback) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Bitmap bmp = mUVCCameraView.captureStillImage();
                Log.e("image", "width:" + bmp.getWidth() + ";height:" + bmp.getHeight() + ";density:" + bmp.getDensity());
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        callback.onDataReceived(bmp);
                    }
                });
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(task, 100, 30);
    }

    private void stopDecoding() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }


}