package vn.edu.usth.camerasystem.alert.stub;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import vn.edu.usth.camerasystem.alert.AlertManager;

/**
 * Stub Day 3 — Armed/sensitivity qua SharedPreferences; alert chỉ Log.
 * P6 thay {@link #showMotionAlert} bằng Notification thật.
 */
public class PrefsAlertManager implements AlertManager {

    private static final String TAG = "AlertManager";
    private static final String PREFS = "camera_security_prefs";
    private static final String KEY_ARMED = "armed";
    private static final String KEY_SENSITIVITY = "sensitivity";
    private static final int DEFAULT_SENSITIVITY = 50;

    private final SharedPreferences prefs;

    public PrefsAlertManager(@NonNull Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public boolean isArmed() {
        return prefs.getBoolean(KEY_ARMED, false);
    }

    @Override
    public void setArmed(boolean armed) {
        prefs.edit().putBoolean(KEY_ARMED, armed).apply();
        Log.d(TAG, "armed=" + armed);
    }

    @Override
    public int getSensitivity() {
        return prefs.getInt(KEY_SENSITIVITY, DEFAULT_SENSITIVITY);
    }

    @Override
    public void setSensitivity(int sensitivity) {
        int clamped = Math.max(0, Math.min(100, sensitivity));
        prefs.edit().putInt(KEY_SENSITIVITY, clamped).apply();
        Log.d(TAG, "sensitivity=" + clamped);
    }

    @Override
    public void showMotionAlert(@NonNull String title, @NonNull String body) {
        if (!isArmed()) {
            Log.d(TAG, "alert skipped — disarmed: " + title);
            return;
        }
        Log.i(TAG, "ALERT: " + title + " — " + body);
    }
}
