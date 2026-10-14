package vn.edu.usth.camerasystem.motion;

/**
 * Callback khi phát hiện chuyển động (P4 gọi → P1/P3/P6 phản ứng).
 */
public interface MotionListener {

    /**
     * @param score       mức độ chuyển động (0..1 hoặc thang do P4 định nghĩa)
     * @param timestampMs thời điểm detect (System.currentTimeMillis())
     */
    void onMotionDetected(float score, long timestampMs);
}
