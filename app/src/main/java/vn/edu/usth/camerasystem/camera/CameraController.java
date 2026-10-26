package vn.edu.usth.camerasystem.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.util.Size;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.TorchState;
import androidx.camera.core.UseCase;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.VideoCapture;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import vn.edu.usth.camerasystem.R;
import vn.edu.usth.camerasystem.motion.MotionAnalyzer;
import vn.edu.usth.camerasystem.ui.AppController;

public class CameraController {

    private static final String TAG = "CameraController";

    public interface Listener {
        void onCameraReady();
        void onCameraError(@StringRes int messageId);
    }

    private final Context context;
    private final AppController appController;
    private final Executor mainExecutor;
    private final AtomicLong frameCount = new AtomicLong();
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private Preview preview;
    private ImageAnalysis imageAnalysis;
    private VideoCapture<Recorder> videoCapture;
    private ExecutorService analysisExecutor;
    private LifecycleOwner lifecycleOwner;
    private PreviewView previewView;
    private Listener listener;
    private volatile int sessionId;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private boolean waitingForRecording;

    public CameraController(@NonNull Context context, @NonNull AppController appController) {
        this.context = context.getApplicationContext();
        this.appController = appController;
        mainExecutor = ContextCompat.getMainExecutor(this.context);
    }

    @MainThread
    public void start(@NonNull LifecycleOwner owner, @NonNull PreviewView view,
                      @NonNull Listener cameraListener) {
        if (lifecycleOwner == owner && camera != null) {
            cameraListener.onCameraReady();
            return;
        }
        stop();
        lifecycleOwner = owner;
        previewView = view;
        listener = cameraListener;
        int currentSession = sessionId;
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(context);
        future.addListener(() -> {
            if (currentSession != sessionId || !canStart()) return;
            try {
                cameraProvider = future.get();
                if (!waitingForRecording) bindCamera();
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                reportError(error);
            } catch (Exception error) {
                reportError(error);
            }
        }, mainExecutor);
    }

    private boolean canStart() {
        return lifecycleOwner != null && previewView != null
                && lifecycleOwner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)
                && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private CameraSelector selectorFor(int facing) {
        return new CameraSelector.Builder().requireLensFacing(facing).build();
    }

    private void bindCamera() {
        if (!canStart() || cameraProvider == null || camera != null) return;
        try {
            CameraSelector selector = selectorFor(lensFacing);
            if (!cameraProvider.hasCamera(selector)) {
                lensFacing = oppositeLens();
                selector = selectorFor(lensFacing);
                if (!cameraProvider.hasCamera(selector)) {
                    listener.onCameraError(R.string.camera_not_available);
                    return;
                }
            }
            preview = new Preview.Builder().build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
            imageAnalysis = new ImageAnalysis.Builder()
                    .setResolutionSelector(new ResolutionSelector.Builder()
                            .setResolutionStrategy(new ResolutionStrategy(new Size(640, 480),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER))
                            .build())
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();
            Recorder recorder = new Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.SD,
                            FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
                    .build();
            videoCapture = VideoCapture.withOutput(recorder);
            analysisExecutor = Executors.newSingleThreadExecutor();
            frameCount.set(0);
            startAnalysis();
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector,
                    preview, imageAnalysis, videoCapture);
            appController.getCameraXRecordingController().ganBoGhi(videoCapture);
            listener.onCameraReady();
        } catch (Exception error) {
            Listener currentListener = listener;
            stop();
            Log.e(TAG, "Không mở được camera", error);
            if (currentListener != null) currentListener.onCameraError(R.string.camera_error);
        }
    }

    private void startAnalysis() {
        int currentSession = sessionId;
        MotionAnalyzer analyzer = new MotionAnalyzer((score, timestamp) ->
                mainExecutor.execute(() -> {
                    if (currentSession == sessionId) appController.onMotionDetected(score, timestamp);
                }), appController.getAlertManager(), appController.getRecordingController());
        imageAnalysis.setAnalyzer(analysisExecutor, image -> {
            if (currentSession != sessionId) {
                image.close();
                return;
            }
            try {
                analyzer.analyze(image);
                frameCount.incrementAndGet();
            } catch (RuntimeException error) {
                Log.e(TAG, "Không phân tích được khung hình", error);
            }
        });
    }

    @MainThread
    public boolean isReady() {
        return camera != null;
    }

    public long getFrameCount() {
        return frameCount.get();
    }

    @MainThread
    public int getLensFacing() {
        return lensFacing;
    }

    @MainThread
    public boolean canSwitchCamera() {
        if (camera == null || appController.getRecordingController().isRecording()) return false;
        try {
            return cameraProvider.hasCamera(selectorFor(oppositeLens()));
        } catch (Exception error) {
            Log.w(TAG, "Không tìm được camera còn lại", error);
            return false;
        }
    }

    @MainThread
    public boolean switchCamera() {
        if (!canSwitchCamera() || lifecycleOwner == null || previewView == null || listener == null) {
            return false;
        }
        LifecycleOwner owner = lifecycleOwner;
        PreviewView view = previewView;
        Listener cameraListener = listener;
        lensFacing = oppositeLens();
        stop();
        start(owner, view, cameraListener);
        return true;
    }

    private int oppositeLens() {
        return lensFacing == CameraSelector.LENS_FACING_BACK
                ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
    }

    @MainThread
    public boolean hasTorch() {
        return camera != null && camera.getCameraInfo().hasFlashUnit();
    }

    @MainThread
    public boolean isTorchOn() {
        return camera != null && Integer.valueOf(TorchState.ON)
                .equals(camera.getCameraInfo().getTorchState().getValue());
    }

    @MainThread
    public boolean toggleTorch() {
        if (!hasTorch()) return false;
        ListenableFuture<Void> future = camera.getCameraControl().enableTorch(!isTorchOn());
        sessionId++;
        startAnalysis();
        int currentSession = sessionId;
        future.addListener(() -> {
            if (currentSession != sessionId || listener == null) return;
            try {
                future.get();
                listener.onCameraReady();
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                listener.onCameraError(R.string.camera_torch_error);
            } catch (Exception error) {
                Log.w(TAG, "Không đổi được trạng thái đèn", error);
                listener.onCameraError(R.string.camera_torch_error);
            }
        }, mainExecutor);
        return true;
    }

    @MainThread
    public void stop() {
        sessionId++;
        if (imageAnalysis != null) imageAnalysis.clearAnalyzer();
        if (analysisExecutor != null) analysisExecutor.shutdown();
        List<UseCase> useCases = new ArrayList<>();
        if (preview != null) useCases.add(preview);
        if (imageAnalysis != null) useCases.add(imageAnalysis);
        if (videoCapture != null) useCases.add(videoCapture);
        UseCase[] oldUseCases = useCases.toArray(new UseCase[0]);
        preview = null;
        imageAnalysis = null;
        videoCapture = null;
        camera = null;
        analysisExecutor = null;
        lifecycleOwner = null;
        previewView = null;
        listener = null;
        if (oldUseCases.length == 0) return;
        if (appController.getRecordingController().isRecording()) {
            waitingForRecording = true;
            appController.stopRecording(new RecordingController.Callback() {
                @Override
                public void onRecordingSaved(@NonNull String filePath) {
                    finishStopping(oldUseCases);
                }

                @Override
                public void onRecordingFailed(@NonNull String reason) {
                    finishStopping(oldUseCases);
                }
            });
        } else {
            unbind(oldUseCases);
        }
    }

    private void finishStopping(UseCase[] useCases) {
        unbind(useCases);
        waitingForRecording = false;
        if (canStart()) bindCamera();
    }

    private void unbind(UseCase[] useCases) {
        if (cameraProvider != null) cameraProvider.unbind(useCases);
        for (UseCase useCase : useCases) {
            if (useCase instanceof Preview) ((Preview) useCase).setSurfaceProvider(null);
        }
        appController.getCameraXRecordingController().ganBoGhi(null);
    }

    private void reportError(Exception error) {
        Log.e(TAG, "Không khởi tạo được camera", error);
        if (listener != null) listener.onCameraError(R.string.camera_error);
    }
}
