package com.mama.sample.camera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import com.mama.sample.R;
import com.mama.sample.base.BaseActivity;
import com.mama.sample.utils.OrientationUtils;
import com.mama.sample.utils.YuvUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Camera2Activity extends BaseActivity {

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    public static final Size[] PREVIEW_SIZES = new Size[]{
            new Size(1920, 1080),
            new Size(1280, 720),
            new Size(1280, 960),
            new Size(640, 480),
            new Size(1440, 1080),
    };

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private final static String TAG = Camera2Activity.class.getSimpleName();

    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_LOCK = 1;
    private static final int STATE_WAITING_PRECAPTURE = 2;
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    private static final int STATE_PICTURE_TAKEN = 4;

    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    final Object mSyncObject = new Object();

    private int mCameraId = 1;
    private int mFacing;

    // view
    private GLSurfaceView mGLSurfaceView;
    private CameraLayout mCameraLayout;
    private Spinner mPreviewSizeSpinner;
    private Spinner mGradualBlurSpinner;
    private View mCaptureView;
    private View mSwitchView;
    private ImageView mGalleryImage;
    // view data
    private Bitmap mGllerayBitmap;
    private int mGradualBlurSpinnerPosition;
    private boolean mSwitchingCamera = false;
    private boolean mIsSurfaceCreated = false;
    private boolean mIsCameraOpened = false;

    // camera
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;

    // preview
    private Camera2Render mRender;
    private Size mPreviewSize;

    // handler
    private Handler mHandler;

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
            mIsCameraOpened = true;
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            mIsCameraOpened = false;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            mIsCameraOpened = false;
        }

    };
    // HandlerThread
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private ImageReader mImageReader;
    private ImageReader mNv21Reader;

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            final Image image = reader.acquireNextImage();
            mBackgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    final byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    Log.i(TAG, "takePicture onPictureTaken data length=" + bytes.length);
                    image.close();
                }
            });
        }

    };

    private int mState = STATE_PREVIEW;

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private boolean mFlashSupported;

    private int mSensorOrientation;

    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    } else {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera2);
        mHandler = new Handler();

        mRender = new Camera2Render(this, new Camera2Render.RenderListener() {
            @Override
            public void onSurfaceCreated() {
                openCamera();
                mIsSurfaceCreated = true;
            }
        });
        initView();

        OrientationUtils.start(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView() {
        // actionbar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setCustomView(R.layout.actionbar);

            View view = actionBar.getCustomView();
            Button btnNext = view.findViewById(R.id.next);
            btnNext.setText(R.string.compare);
            btnNext.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        mRender.showOriginal(true);
                        Log.e(TAG, "mama= ACTION_DOWN");
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        mRender.showOriginal(false);
                        Log.e(TAG, "mama= ACTION_UP");
                    }
                    return false;
                }
            });
        }

        // GL
        mCameraLayout = (CameraLayout) findViewById(R.id.camera_layout);

        mGLSurfaceView = (GLSurfaceView) findViewById(R.id.glsurfaceview);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setRenderer(mRender);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        mGLSurfaceView.requestRender();

        // switch camera
        mSwitchView = findViewById(R.id.camera_preview_switch);
        mSwitchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSwitchingCamera = true;
                closeCamera();
                mRender.switchCamera();
                mCameraId ^= 1;
                openCamera();

                initCameraSizeSpinner();
                setButtonsAvailableDelay();
            }
        });

        mCaptureView = findViewById(R.id.camera_take_picture);
        mCaptureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                v.setEnabled(false);
                setButtonsAvailableDelay();
                takePicture();
            }
        });

        // TODO: gallery
//        mGalleryImage = (ImageView) findViewById(R.id.camera_gallery);
//        mGalleryImage.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (mGllerayBitmap != null) {
//                    Intent intent = new Intent(getBaseContext(), PictureActivity.class);
//                    startActivity(intent);
//                }
//            }
//        });
//        String path = FileUtil.getLatestPhotoPathFromDir(Share.IMAGE_SAVE_DIR);
//        if (path != null) {
//            mGllerayBitmap = BitmapUtil.decodeSampledBitmapFromPath(path, 512, 512);
//            mGalleryImage.setImageBitmap(mGllerayBitmap);
//        }

        initCameraSizeSpinner();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
//            case android.R.id.undo:
//                onBackPressed();
//                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    class LogThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (mDebug && ! isInterrupted()) {

                runOnUiThread(new Runnable() {
                    public void run() {
                        String msg =
                                "fps: " + mRender.getFPS() + "\n";
                        mLogView.setText(msg);
                    }
                });
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private LogThread logThread = null;
    private TextView mLogView;
    private boolean mDebug = true;
    private void debug() {
        mLogView = (TextView) findViewById(R.id.log_textview);
        logThread = new LogThread();
        logThread.start();
    }

    private void initCameraSizeSpinner() {
        CameraCharacteristics characteristics = null;
        try {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            characteristics = manager.getCameraCharacteristics(mCameraId+"");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        if (characteristics == null) {
            return;
        }
        final StreamConfigurationMap map = characteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            return;
        }
        // support size
        final List<Size> sizes = new ArrayList<Size>() {
            {
                addAll(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)));
            }
        };
        // show size
        final List<Size> showSizes = new ArrayList<Size>() {
            {
                for (Size s: PREVIEW_SIZES) {
                    if (sizes.contains(s)) {
                        add(s);
                    }
                }
            }
        };

        String[] items = new String[showSizes.size()];
        for (int i = 0; i < items.length; i++) {
            Size size = showSizes.get(i);
            items[i] = size.getWidth() + "x" + size.getHeight();
        }

        mCameraLayout = findViewById(R.id.camera_layout);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPreviewSizeSpinner = findViewById(R.id.preview_size_spinner);
        mPreviewSizeSpinner.setAdapter(adapter);
        mPreviewSizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, final View view, final int position, long id) {
                if (mHandler != null) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setButtonsAvailableDelay();
                            final Size size = showSizes.get(position);
                            mGLSurfaceView.queueEvent(new Runnable() {
                                public void run() {
                                    // change size
                                    int width  = size.getWidth();
                                    int height = size.getHeight();
                                    closeCamera();
                                    mPreviewSize = new Size(width, height);
                                    openCamera();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mCameraLayout.requestLayoutFromSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                                        }
                                    });
                                }
                            });
                        }
                    }, 200);
                }

            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        final int defaultIndex = 0;
        final Size defaultSize = showSizes.get(defaultIndex);
        mPreviewSize = new Size(defaultSize.getWidth(), defaultSize.getHeight());
        mCameraLayout.requestLayoutFromSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

        mPreviewSizeSpinner.setSelection(defaultIndex);
    }

    private void setButtonsAvailableDelay() {
        mCaptureView.setEnabled(false);
        mSwitchView.setEnabled(false);
        if (mHandler != null) {
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    mCaptureView.setEnabled(true);
                    mSwitchView.setEnabled(true);
                }
            }, 2000);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (mIsSurfaceCreated && ! mIsCameraOpened) {
            openCamera();
        }
        debug();
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
        logThread.interrupt();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRender.destroy();
        mHandler.removeCallbacks(null);
        mHandler = null;
        OrientationUtils.stop();
    }

    /**
     * Sets up member variables related to camera.
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int cameraId) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics
                    = manager.getCameraCharacteristics(cameraId + "");

            // We don't use a front facing camera in this sample.
            mFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                return;
            }

            // For still image captures, we use the largest available size.
            Size largest = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                    new CompareSizesByArea());
            mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                    ImageFormat.JPEG, /*maxImages*/2);
            mImageReader.setOnImageAvailableListener(
                    mOnImageAvailableListener, mBackgroundHandler);

            // Find out if we need to swap dimension to get the preview size relative to sensor
            // coordinate.
            //noinspection ConstantConditions
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

            mNv21Reader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                    ImageFormat.YUV_420_888, 10);
            mNv21Reader.setOnImageAvailableListener(
                    new ImageReader.OnImageAvailableListener() {
                        @Override
                        public void onImageAvailable(final ImageReader reader) {
                            Image image = reader.acquireNextImage();
                            long now = System.currentTimeMillis();
                            byte[] nv21 = YuvUtils.YUV_420_888toNV21(
                                    image.getPlanes()[0].getBuffer(),
                                    image.getPlanes()[1].getBuffer(),
                                    image.getPlanes()[2].getBuffer());

                            Log.e(TAG, "Time: YUV_420_888toNV21: " + (System.currentTimeMillis() - now) + "ms");
                            int width  = image.getWidth();
                            int height = image.getHeight();
                            // render
                            mRender.updateFrame(nv21, width, height, isFrontCamera());
                            image.close();
                        }
                    }, mBackgroundHandler);
            // Init OES

            // Check if the flash is supported.
            Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            mFlashSupported = available == null ? false : available;
            return;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            showToast(getString(R.string.camera_error));
        }
    }

    /**
     * Opens the camera mCameraId.
     */
    private void openCamera() {
        if (mIsCameraOpened)
            return;
        Log.e(TAG, "openCamera");
        if (! checkPermission(null)) {
            return;
        }
        setUpCameraOutputs(mCameraId);
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId+"", mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
            if (null != mNv21Reader) {
                mNv21Reader.close();
                mNv21Reader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
            mIsCameraOpened = false;
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {
            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(mNv21Reader.getSurface());

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(mImageReader.getSurface(), mNv21Reader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }

                            // if switching camera
                            if (mSwitchingCamera) {
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        mRender.finishSwitchCamera();
                                        mSwitchingCamera = false;
                                    }
                                }, 200);
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initiate a still image capture.
     */
    private void takePicture() {
        lockFocus();
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {
        try {
            if (null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    private boolean isFrontCamera() {
        return mFacing == CameraCharacteristics.LENS_FACING_FRONT;
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }
}

