package vn.edu.usth.camerasystem.alert;

import androidx.annotation.NonNull;

/**
 * Cảnh báo + trạng thái Armed (P6 implement bằng Notification + Prefs).
 */
public interface AlertManager {

    boolean isArmed();

    void setArmed(boolean armed);

    /**
     * Độ nhạy motion 0..100 (map sang threshold bên P4).
     */
    int getSensitivity();

    void setSensitivity(int sensitivity);

    void showMotionAlert(@NonNull String title, @NonNull String body);
}
