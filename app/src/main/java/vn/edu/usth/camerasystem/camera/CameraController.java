package vn.edu.usth.camerasystem.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import vn.edu.usth.camerasystem.R;

/** Controls the P2 camera preview on the Monitor screen. */
public class CameraController {

    private static final String TAG = "CameraController";

    public interface Listener {
        void onCameraReady();
        void onCameraError(@StringRes int messageId);
    }

    private final Context context;
    private final Executor mainExecutor;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private Preview preview;
    private LifecycleOwner lifecycleOwner;
    private PreviewView previewView;
    private Listener listener;
    private int sessionId;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;

    public CameraController(@NonNull Context context) {
        this.context = context.getApplicationContext();
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
                bindCamera();
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                reportError(error);
            } catch (ExecutionException | RuntimeException error) {
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
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview);
            listener.onCameraReady();
            Log.i(TAG, "Bound preview, lens=" + lensFacing);
        } catch (Exception error) {
            Listener currentListener = listener;
            stop();
            Log.e(TAG, "Cannot bind the camera preview", error);
            if (currentListener != null) currentListener.onCameraError(R.string.camera_error);
        }
    }

    @MainThread
    public boolean isReady() {
        return camera != null;
    }

    @MainThread
    public int getLensFacing() {
        return lensFacing;
    }

    @MainThread
    public boolean canSwitchCamera() {
        if (cameraProvider == null || camera == null) return false;
        try {
            return cameraProvider.hasCamera(selectorFor(oppositeLens()));
        } catch (Exception error) {
            Log.w(TAG, "Cannot query the other lens", error);
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
    public void stop() {
        sessionId++;
        Preview oldPreview = preview;
        preview = null;
        camera = null;
        lifecycleOwner = null;
        previewView = null;
        listener = null;
        if (cameraProvider != null && oldPreview != null) cameraProvider.unbind(oldPreview);
        if (oldPreview != null) oldPreview.setSurfaceProvider(null);
    }

    private void reportError(Exception error) {
        Log.e(TAG, "Camera initialization failed", error);
        if (listener != null) listener.onCameraError(R.string.camera_error);
    }
}
