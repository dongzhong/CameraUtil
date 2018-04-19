package dongzhong.camerautil;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    private static final int PERMISSION_REQUEST_CODE_CAMERA = 0x001;

    private ImageView imageView;
    private Button startButton;
    private Button stopButton;

    private CameraUtil cameraUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {
        cameraUtil = CameraUtil.getInstance(this, 640, 480, new CameraUtil.CameraImageListener() {
            @Override
            public void onImageReturn(Image image) {
                ByteBuffer bufferY = image.getPlanes()[0].getBuffer();
                ByteBuffer bufferUV = image.getPlanes()[2].getBuffer();
                ByteBuffer outBuffer = ByteBuffer.allocate((int) (image.getCropRect().width() * image.getCropRect().height() * 1.5));
                outBuffer.put(bufferY);
                outBuffer.put(bufferUV);
                YuvImage yuvImage = new YuvImage(outBuffer.array(), ImageFormat.NV21, image.getCropRect().width(), image.getCropRect().height(), null);
                final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(image.getCropRect(), 100, byteArrayOutputStream);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        byte[] data = byteArrayOutputStream.toByteArray();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        imageView.setImageBitmap(bitmap);
                    }
                });
            }

            @Override
            public void onCameraDisconnected(String cameraId) {
                Log.d(TAG, "CameraDisconnected: " + cameraId);
            }

            @Override
            public void onCameraError(String info) {
                Log.d(TAG, "CameraError: " + info);
            }
        });

        imageView = (ImageView) findViewById(R.id.preview_imageview);

        startButton = (Button) findViewById(R.id.start_preview);
        startButton.setOnClickListener(this);

        stopButton = (Button) findViewById(R.id.stop_preview);
        stopButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start_preview:
                try {
                    cameraUtil.startCamera();
                }
                catch (SecurityException e) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA)) {

                        }
                        else {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE_CAMERA);
                        }
                    }
                }
                break;
            case R.id.stop_preview:
                cameraUtil.release();
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE_CAMERA:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraUtil.startCamera();
                }
                else {
                    Log.d(TAG, "权限被拒绝");
                }
                break;
            default:
                break;
        }
    }
}
