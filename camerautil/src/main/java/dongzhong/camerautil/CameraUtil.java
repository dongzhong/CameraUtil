package dongzhong.camerautil;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;

import java.util.Arrays;

/**
 * Created by dongzhong on 2018/4/18.
 */

public class CameraUtil {
    private static final String TAG = "CameraUtil";

    private static CameraUtil instance;

    private Context context;
    private CameraImageListener cameraImageListener;

    private CameraManager cameraManager;
    private CameraCaptureSession cameraCaptureSession;
    private CameraDevice cameraDevice;

    private ImageReader imageReader;

    private HandlerThread thread;
    private Handler handler;

    private CameraUtil(@NonNull Context context, int width, int height, CameraImageListener cameraImageListener) {
        this.context = context;
        this.cameraImageListener = cameraImageListener;

        thread = new HandlerThread("CameraUtil");
        thread.start();
        handler = new Handler(thread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                return false;
            }
        });

        initImageReader(width, height);

        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    public static CameraUtil getInstance(Context context, int width, int height, CameraImageListener cameraImageListener) {
        if (instance != null) {
            synchronized (CameraUtil.class) {
                if (instance != null) {
                    instance.release();
                    instance = null;
                }
            }
        }
        if (context == null) {
            return null;
        }
        if (instance == null) {
            synchronized (CameraUtil.class) {
                if (instance == null) {
                    instance = new CameraUtil(context, width, height, cameraImageListener);
                }
            }
        }

        return instance;
    }

    /**
     * 初始化ImageReader
     */
    private void initImageReader(int width, int height) {
        imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 1);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireNextImage();
                if (cameraImageListener != null) {
                    cameraImageListener.onImageReturn(image);
                }
                image.close();
            }
        }, handler);
    }

    /**
     * 开启摄像头
     */
    public void startCamera() throws SecurityException {
        try {
            if (cameraManager == null) {
                return;
            }
            String[] cameraIdList = cameraManager.getCameraIdList();
            if (cameraIdList == null || cameraIdList.length <= 0) {
                return;
            }

            cameraManager.openCamera(cameraIdList[0], new CameraDeviceStateCallback(), handler);
        }
        catch (Exception e) {

        }
    }

    /**
     * 释放资源
     */
    public void release() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    /**
     * 开启摄像头状态回调类
     */
    private class CameraDeviceStateCallback extends CameraDevice.StateCallback {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            try {
                cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), new CameraCaptureSessionStatecallback(), handler);
            }
            catch (Exception e) {

            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            if (cameraImageListener != null) {
                cameraImageListener.onCameraDisconnected(camera.getId());
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    }

    /**
     * 会话创建状态回调类
     */
    private class CameraCaptureSessionStatecallback extends CameraCaptureSession.StateCallback {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            cameraCaptureSession = session;
            sendRepeatPreviewRequest(cameraCaptureSession);
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    }

    /**
     * 发送连续预览请求
     *
     * @param cameraCaptureSession
     */
    private void sendRepeatPreviewRequest(CameraCaptureSession cameraCaptureSession) {
        try {
            CameraDevice device = cameraCaptureSession.getDevice();
            CaptureRequest.Builder builder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(imageReader.getSurface());
            builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_OFF);
            cameraCaptureSession.setRepeatingRequest(builder.build(), new CameraCaptureSession.CaptureCallback() {}, handler);
        }
        catch (Exception e) {

        }
    }

    /**
     * 相机图像回调监听
     */
    public interface CameraImageListener {
        /**
         * 图像返回: ImageFormat.YUV_420_888格式
         *
         * @param image
         */
        void onImageReturn(Image image);

        /**
         * 摄像头断开连接
         *
         * @param cameraId
         */
        void onCameraDisconnected(String cameraId);
    }
}
