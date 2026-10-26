package vn.edu.usth.camerasystem.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * P1 Day 4 — đồng bộ Armed / Recording / Sensitivity giữa Monitor và Settings.
 */
public class SecurityViewModel extends ViewModel implements AppController.StateListener {

    private final MutableLiveData<Boolean> armed = new MutableLiveData<>();
    private final MutableLiveData<Boolean> recording = new MutableLiveData<>();
    private final MutableLiveData<Integer> sensitivity = new MutableLiveData<>();

    public SecurityViewModel() {
        AppController.get().addStateListener(this);
    }

    @NonNull
    public LiveData<Boolean> getArmed() {
        return armed;
    }

    @NonNull
    public LiveData<Boolean> getRecording() {
        return recording;
    }

    @NonNull
    public LiveData<Integer> getSensitivity() {
        return sensitivity;
    }

    public void setArmed(boolean value) {
        AppController.get().getAlertManager().setArmed(value);
    }

    public void setSensitivity(int value) {
        AppController.get().getAlertManager().setSensitivity(value);
    }

    public void simulateMotion() {
        AppController controller = AppController.get();
        float score = controller.getAlertManager().getSensitivity() / 100f;
        controller.onMotionDetected(score, System.currentTimeMillis());
    }

    @Override
    public void onArmedChanged(boolean value) {
        armed.postValue(value);
    }

    @Override
    public void onRecordingChanged(boolean value) {
        recording.postValue(value);
    }

    @Override
    public void onSensitivityChanged(int value) {
        sensitivity.postValue(value);
    }

    @Override
    protected void onCleared() {
        AppController.get().removeStateListener(this);
        super.onCleared();
    }
}
