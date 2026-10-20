package vn.edu.usth.camerasystem.ui;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import vn.edu.usth.camerasystem.alert.AlertManager;
import vn.edu.usth.camerasystem.alert.stub.PrefsAlertManager;
import vn.edu.usth.camerasystem.camera.RecordingController;
import vn.edu.usth.camerasystem.camera.CameraXRecordingController;
import vn.edu.usth.camerasystem.motion.MotionListener;
import vn.edu.usth.camerasystem.storage.EventRepository;
import vn.edu.usth.camerasystem.storage.SecurityEvent;
import vn.edu.usth.camerasystem.storage.RoomEventRepository;

/**
 * Điều phối module chung (P1).
 * <p>
 * Pipeline mục tiêu Day 7:<br>
 * Armed + motion → record → save event → notify
 * <p>
 * P2–P6 thay stub bằng implement thật; giữ nguyên các getter này.
 */
public final class AppController implements MotionListener {

    private static final String TAG = "AppController";
    private static final long RECORD_DURATION_MS = TimeUnit.SECONDS.toMillis(8);
    private static final long MOTION_COOLDOWN_MS = TimeUnit.SECONDS.toMillis(10);

    private static AppController instance;

    private final AlertManager alertManager;
    private final EventRepository eventRepository;
    private final CameraXRecordingController recordingController;

    private long lastMotionHandledAt;

    private AppController(@NonNull Context context) {
        Context app = context.getApplicationContext();
        alertManager = new PrefsAlertManager(app);
        eventRepository = new RoomEventRepository(app);
        recordingController = new CameraXRecordingController(app,
                androidx.core.content.ContextCompat.getMainExecutor(app));
    }

    public static synchronized void init(@NonNull Context context) {
        if (instance == null) {
            instance = new AppController(context);
        }
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

    /** P2 dùng để gắn VideoCapture sau khi bind camera. */
    @NonNull
    public CameraXRecordingController getCameraXRecordingController() {
        return recordingController;
    }

    /**
     * P4 gọi qua listener này khi detect motion.
     */
    @Override
    public void onMotionDetected(float score, long timestampMs) {
        if (!alertManager.isArmed()) {
            Log.d(TAG, "motion ignored — disarmed score=" + score);
            return;
        }
        if (recordingController.isRecording()) {
            Log.d(TAG, "motion ignored — already recording");
            return;
        }
        if (timestampMs - lastMotionHandledAt < MOTION_COOLDOWN_MS) {
            Log.d(TAG, "motion ignored — cooldown");
            return;
        }

        lastMotionHandledAt = timestampMs;
        Log.i(TAG, "motion accepted score=" + score);

        boolean started = recordingController.startRecording();
        if (!started) {
            return;
        }

        alertManager.showMotionAlert(
                "Phát hiện chuyển động",
                "Đang ghi clip (điểm " + String.format("%.2f", score) + ")"
        );

        // Stub: tự stop sau vài giây. P3 có thể thay bằng timer/CameraX callback thật.
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() ->
                recordingController.stopRecording(new RecordingController.Callback() {
                    @Override
                    public void onRecordingSaved(@NonNull String filePath) {
                        SecurityEvent event = new SecurityEvent(
                                UUID.randomUUID().toString(),
                                System.currentTimeMillis(),
                                filePath,
                                null,
                                "Điểm chuyển động: " + score
                        );
                        eventRepository.addEvent(event);
                        alertManager.showMotionAlert(
                                "Phát hiện chuyển động",
                                "Đã lưu clip"
                        );
                        Log.i(TAG, "event saved path=" + filePath);
                    }

                    @Override
                    public void onRecordingFailed(@NonNull String reason) {
                        Log.w(TAG, "Ghi video thất bại: " + reason);
                    }
                }), RECORD_DURATION_MS);
    }
}
