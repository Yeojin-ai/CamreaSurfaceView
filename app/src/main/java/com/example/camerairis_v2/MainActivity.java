package com.example.camerairis_v2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.logging.LogRecord;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    static final String TAG = "irisSurfaceCamera";

    public String CamID = "0";
    private CameraDevice mCameraDevice;
    private CaptureRequest captureRequest;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mSession;
    private SurfaceView surfaceView;
    private SurfaceHolder mSurfaceHolder;

    //Variables for Save Image
    private Handler mHandler;
    private ImageReader mImageReader;

    private static final SparseArray ORIENTATIONS = new SparseArray();
    static {
        ORIENTATIONS.append(ExifInterface.ORIENTATION_NORMAL, 0);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_90, 90);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_180, 180);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_270, 270);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"iris onCreate1");

        setContentView(R.layout.activity_main);
        /*
        ImageButton button = findViewById(R.id.take_photo);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });*/
        surfaceView = findViewById(R.id.surfaceView);
        Toast.makeText(this,"onCreate",Toast.LENGTH_SHORT).show();
        Log.d(TAG,"iris onCreate2");

        initSufaceView();

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void initSufaceView(){
        Log.d(TAG,"iris initSurfaceView");
        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback(){
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                Log.d(TAG,"iris surface Created");
                initCamPrev();

            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG,"iris surface Changed");

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                Log.d(TAG,"iris surface Destroyed");
                if (mCameraDevice != null){
                    mCameraDevice.close();
                    mCameraDevice = null;
                }

            }
        });   //add callback at holder.
    }

    public void initCamPrev(){
        Log.d(TAG,"iris initCamPrev");
        HandlerThread handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        Handler mainHandler = new Handler(getMainLooper());
        openCamera();
    }

    public void openCamera(){
        Log.d(TAG,"iris openCamera");
        CameraManager mCameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            //create Instance
            CameraCharacteristics cc = mCameraManager.getCameraCharacteristics(CamID);
            StreamConfigurationMap scmap = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size mPrviewSize = scmap.getOutputSizes(ImageFormat.JPEG)[0];

            Log.i("LargestSize", "iris"+mPrviewSize.getWidth()+" "+mPrviewSize.getHeight());
            //setAspectRatioView(mPrviewSize.getHeight(), mPrviewSize.getWidth());

            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
                return;
            }
            mCameraManager.openCamera(CamID,deviceStateCallback,mHandler);
        } catch (CameraAccessException e) {
            Toast.makeText(this,"couldn't open camera.",Toast.LENGTH_SHORT).show();
            throw new RuntimeException(e);
        }
    }

    private void setAspectRatioView(int height, int width) {
    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
            //new SaveImageTask().excute(bitmap);
        }
    };
    private CameraDevice.StateCallback deviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG,"iris deviceStateCallback onOpened");
            mCameraDevice = camera;
            try{
                takePreview();
            }catch (CameraAccessException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            if (mCameraDevice != null){
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Toast.makeText(MainActivity.this,"couldn't open camera.",Toast.LENGTH_SHORT).show();
        }
    };

    public void takePreview() throws CameraAccessException{
        Log.d(TAG,"iris takePreview");
        Surface previewSurface = mSurfaceHolder.getSurface();
        //Surface imageSurface = mImageReader.getSurface();

        mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mPreviewBuilder.addTarget(previewSurface);
        mCameraDevice.createCaptureSession(Arrays.asList(previewSurface),mSessionPreviewStateCallback, mHandler);

    }
    public CameraCaptureSession.StateCallback mSessionPreviewStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.d(TAG,"iris onConfigured");
            if(null==mCameraDevice)return;
            mSession = session;
            //Toast.makeText(MainActivity.this, "ddd",Toast.LENGTH_SHORT).show();
            try {
                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                //mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                mSession.setRepeatingRequest(mPreviewBuilder.build(),mSessionCaptureCallback,mHandler);
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Toast.makeText(MainActivity.this, "camera open failed",Toast.LENGTH_SHORT).show();
        }
    };
    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            mSession = session;
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            mSession = session;
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }
    };
    public void takePicture(){

    }
}

