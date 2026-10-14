package vn.edu.usth.camerasystem.camera.stub;

import android.util.Log;

import androidx.annotation.Nullable;

import vn.edu.usth.camerasystem.camera.RecordingController;

/**
 * Stub Day 3 — P3 thay bằng CameraX VideoCapture thật.
 */
public class NoOpRecordingController implements RecordingController {

    private static final String TAG = "RecordingController";

    private boolean recording;

    @Override
    public boolean isRecording() {
        return recording;
    }

    @Override
    public boolean startRecording() {
        if (recording) {
            Log.d(TAG, "startRecording ignored — already recording");
            return false;
        }
        recording = true;
        Log.d(TAG, "startRecording (stub)");
        return true;
    }

    @Override
    public void stopRecording(@Nullable Callback callback) {
        if (!recording) {
            if (callback != null) {
                callback.onRecordingFailed("Not recording");
            }
            return;
        }
        recording = false;
        Log.d(TAG, "stopRecording (stub)");
        if (callback != null) {
            callback.onRecordingFailed("Stub — no real video file yet (P3)");
        }
    }
}
