package vn.edu.usth.camerasystem.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import vn.edu.usth.camerasystem.alert.AlertManager;
import vn.edu.usth.camerasystem.alert.stub.PrefsAlertManager;
import vn.edu.usth.camerasystem.camera.CameraController;
import vn.edu.usth.camerasystem.camera.CameraXRecordingController;
import vn.edu.usth.camerasystem.camera.RecordingController;
import vn.edu.usth.camerasystem.motion.MotionListener;
import vn.edu.usth.camerasystem.storage.EventRepository;
import vn.edu.usth.camerasystem.storage.SecurityEvent;
import vn.edu.usth.camerasystem.storage.RoomEventRepository;

public final class AppController implements MotionListener {

    private static final String TAG = "AppController";
    private static final long RECORD_DURATION_MS = TimeUnit.SECONDS.toMillis(8);
    private static final long MOTION_COOLDOWN_MS = TimeUnit.SECONDS.toMillis(10);
    private static AppController instance;

    private final AlertManager alertManager;
    private final EventRepository eventRepository;
    private final CameraXRecordingController recordingController;
    private final CameraController cameraController;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoStop = () -> stopRecording(null);
    private final List<RecordingController.Callback> stopCallbacks = new ArrayList<>();
    private final List<StateListener> stateListeners = new CopyOnWriteArrayList<>();
    private long lastMotionHandledAt;
    private Float motionScore;
    private boolean stoppingRecording;

    private AppController(@NonNull Context context) {
        Context app = context.getApplicationContext();
        alertManager = new PrefsAlertManager(app);
        eventRepository = new RoomEventRepository(app);
        recordingController = new CameraXRecordingController(app,
                androidx.core.content.ContextCompat.getMainExecutor(app));
        cameraController = new CameraController(app, this);
        alertManager.setChangeListener(new AlertManager.ChangeListener() {
            @Override
            public void onArmedChanged(boolean armed) {
                notifyArmed(armed);
            }

            @Override
            public void onSensitivityChanged(int sensitivity) {
                notifySensitivity(sensitivity);
            }
        });
    }

    public static synchronized void init(@NonNull Context context) {
        if (instance == null) instance = new AppController(context);
    }

    @NonNull
    public static AppController get() {
        if (instance == null) {
            throw new IllegalStateException("Call AppController.init() in Application.onCreate()");
        }
        return instance;
    }

    @NonNull
    public AlertManager getAlertManager() {
        return alertManager;
    }

    @NonNull
    public EventRepository getEventRepository() {
        return eventRepository;
    }

    @NonNull
    public RecordingController getRecordingController() {
        return recordingController;
    }

    @NonNull
    public CameraXRecordingController getCameraXRecordingController() {
        return recordingController;
    }

    @NonNull
    public CameraController getCameraController() {
        return cameraController;
    }

    public void addStateListener(@NonNull StateListener listener) {
        if (!stateListeners.contains(listener)) {
            stateListeners.add(listener);
        }
        listener.onArmedChanged(alertManager.isArmed());
        listener.onRecordingChanged(recordingController.isRecording());
        listener.onSensitivityChanged(alertManager.getSensitivity());
    }

    public void removeStateListener(@NonNull StateListener listener) {
        stateListeners.remove(listener);
    }

    public boolean startRecording() {
        if (recordingController.isRecording() || stoppingRecording) return false;
        handler.removeCallbacks(autoStop);
        motionScore = null;
        boolean started = recordingController.startRecording();
        if (started) {
            notifyRecording(true);
        }
        return started;
    }

    public void stopRecording(@Nullable RecordingController.Callback callback) {
        handler.removeCallbacks(autoStop);
        if (callback != null) stopCallbacks.add(callback);
        if (stoppingRecording) return;
        if (!recordingController.isRecording()) {
            finishRecording(null, "Hiện không có video đang ghi");
            return;
        }
        stoppingRecording = true;
        try {
            recordingController.stopRecording(new RecordingController.Callback() {
                @Override
                public void onRecordingSaved(@NonNull String filePath) {
                    finishRecording(filePath, null);
                }

                @Override
                public void onRecordingFailed(@NonNull String reason) {
                    finishRecording(null, reason);
                }
            });
        } catch (RuntimeException error) {
            Log.e(TAG, "Không dừng được video", error);
            finishRecording(null, "Không dừng được video");
        }
    }

    private void finishRecording(@Nullable String filePath, @Nullable String reason) {
        Float savedScore = motionScore;
        motionScore = null;
        stoppingRecording = false;
        notifyRecording(false);
        if (filePath != null && savedScore != null) {
            eventRepository.addEvent(new SecurityEvent(UUID.randomUUID().toString(),
                    System.currentTimeMillis(), filePath, null,
                    "Điểm chuyển động: " + savedScore));
            alertManager.showMotionAlert("Phát hiện chuyển động", "Đã lưu clip");
        }
        List<RecordingController.Callback> callbacks = new ArrayList<>(stopCallbacks);
        stopCallbacks.clear();
        for (RecordingController.Callback callback : callbacks) {
            if (filePath != null) callback.onRecordingSaved(filePath);
            else callback.onRecordingFailed(reason == null ? "Không lưu được video" : reason);
        }
        if (reason != null) Log.w(TAG, reason);
    }

    @Override
    public void onMotionDetected(float score, long timestampMs) {
        if (!alertManager.isArmed() || recordingController.isRecording()
                || timestampMs - lastMotionHandledAt < MOTION_COOLDOWN_MS) return;
        if (!startRecording()) return;
        motionScore = score;
        lastMotionHandledAt = timestampMs;
        handler.postDelayed(autoStop, RECORD_DURATION_MS);
        Log.i(TAG, "motion accepted score=" + score);
    }

    private void notifyArmed(boolean armed) {
        for (StateListener listener : stateListeners) {
            listener.onArmedChanged(armed);
        }
    }

    private void notifyRecording(boolean recording) {
        for (StateListener listener : stateListeners) {
            listener.onRecordingChanged(recording);
        }
    }

    private void notifySensitivity(int sensitivity) {
        for (StateListener listener : stateListeners) {
            listener.onSensitivityChanged(sensitivity);
        }
    }

    public interface StateListener {
        void onArmedChanged(boolean armed);

        void onRecordingChanged(boolean recording);

        void onSensitivityChanged(int sensitivity);
    }
}
