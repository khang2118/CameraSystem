package vn.edu.usth.camerasystem.camera;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Điều khiển ghi video (P3 implement).
 * Pipeline Day 7: motion → {@link #startRecording()} → lưu event.
 */
public interface RecordingController {

    boolean isRecording();

    /**
     * Bắt đầu ghi. Nếu đang ghi thì bỏ qua (chống double-start).
     *
     * @return true nếu bắt đầu ghi mới; false nếu đang ghi / không sẵn sàng
     */
    boolean startRecording();

    /**
     * Dừng ghi và trả về đường dẫn file (nếu có).
     */
    void stopRecording(@Nullable Callback callback);

    interface Callback {
        void onRecordingSaved(@NonNull String filePath);

        void onRecordingFailed(@NonNull String reason);
    }
}
