package vn.edu.usth.camerasystem.ui.monitor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.CameraSelector;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import vn.edu.usth.camerasystem.R;
import vn.edu.usth.camerasystem.alert.AlertManager;
import vn.edu.usth.camerasystem.camera.CameraController;
import vn.edu.usth.camerasystem.camera.RecordingController;
import vn.edu.usth.camerasystem.ui.AppController;

public class MonitorFragment extends Fragment implements CameraController.Listener {

    private TextView statusChip;
    private TextView cameraStatus;
    private PreviewView previewView;
    private MaterialButton btnCameraPermission;
    private MaterialButton btnCameraSettings;
    private MaterialButton btnSwitchCamera;
    private MaterialButton btnTorch;
    private MaterialButton btnTestRecord;
    private AppController appController;
    private AlertManager alertManager;
    private CameraController cameraController;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable stopTestRecording = this::stopRecording;
    private final Runnable updateStatus = new Runnable() {
        @Override
        public void run() {
            refreshControls();
            handler.postDelayed(this, 500);
        }
    };

    private final ActivityResultLauncher<String> cameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (getView() == null) return;
                refreshCamera();
                if (!granted) cameraStatus.setText(R.string.camera_permission_denied);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_monitor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        appController = AppController.get();
        alertManager = appController.getAlertManager();
        cameraController = appController.getCameraController();
        statusChip = view.findViewById(R.id.status_chip);
        cameraStatus = view.findViewById(R.id.camera_status);
        previewView = view.findViewById(R.id.preview_view);
        btnCameraPermission = view.findViewById(R.id.btn_camera_permission);
        btnCameraSettings = view.findViewById(R.id.btn_camera_settings);
        btnSwitchCamera = view.findViewById(R.id.btn_switch_camera);
        btnTorch = view.findViewById(R.id.btn_camera_torch);
        btnTestRecord = view.findViewById(R.id.btn_test_record);

        btnCameraPermission.setOnClickListener(v -> requestCameraPermission());
        btnCameraSettings.setOnClickListener(v -> startActivity(new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + requireContext().getPackageName()))));
        btnSwitchCamera.setOnClickListener(v -> {
            if (!cameraController.switchCamera()) {
                showMessage(getString(appController.getRecordingController().isRecording()
                        ? R.string.camera_recording_busy : R.string.camera_not_available));
            } else cameraStatus.setText(R.string.camera_loading);
            refreshControls();
        });
        btnTorch.setOnClickListener(v -> {
            if (!cameraController.toggleTorch()) showMessage(getString(R.string.camera_torch_unavailable));
        });
        btnTestRecord.setOnClickListener(v -> {
            if (appController.getRecordingController().isRecording()) stopRecording();
            else if (appController.startRecording()) handler.postDelayed(stopTestRecording, 5000);
            else showMessage(getString(R.string.recording_start_failed));
            refreshControls();
        });
        view.findViewById(R.id.btn_toggle_arm).setOnClickListener(v -> {
            alertManager.setArmed(!alertManager.isArmed());
            refreshControls();
        });
        view.findViewById(R.id.btn_simulate_motion).setOnClickListener(v -> {
            if (!alertManager.isArmed()) {
                showMessage(getString(R.string.monitor_status_disarmed));
                return;
            }
            appController.onMotionDetected(alertManager.getSensitivity() / 100f,
                    System.currentTimeMillis());
            refreshControls();
        });
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        if (hasCameraPermission()) refreshCamera();
        else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.camera_permission_title)
                    .setMessage(R.string.camera_permission_reason)
                    .setPositiveButton(R.string.camera_permission_grant,
                            (dialog, which) -> cameraPermission.launch(Manifest.permission.CAMERA))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else cameraPermission.launch(Manifest.permission.CAMERA);
    }

    private void refreshCamera() {
        if (previewView == null || isHidden()) return;
        boolean granted = hasCameraPermission();
        btnCameraPermission.setText(R.string.camera_permission_grant);
        btnCameraPermission.setVisibility(granted ? View.GONE : View.VISIBLE);
        btnCameraSettings.setVisibility(granted ? View.GONE : View.VISIBLE);
        if (!granted) {
            cameraController.stop();
            cameraStatus.setText(R.string.camera_permission_needed);
        } else {
            cameraStatus.setText(R.string.camera_loading);
            cameraController.start(getViewLifecycleOwner(), previewView, this);
        }
        refreshControls();
    }

    private void refreshControls() {
        if (cameraStatus == null) return;
        boolean armed = alertManager.isArmed();
        statusChip.setText(armed ? R.string.monitor_status_armed : R.string.monitor_status_disarmed);
        statusChip.setBackgroundResource(armed ? R.drawable.bg_status_chip_armed : R.drawable.bg_status_chip);
        boolean ready = cameraController.isReady();
        boolean recording = appController.getRecordingController().isRecording();
        btnSwitchCamera.setEnabled(cameraController.canSwitchCamera());
        btnTorch.setEnabled(cameraController.hasTorch());
        btnTorch.setText(!ready ? R.string.camera_torch_on
                : !cameraController.hasTorch() ? R.string.camera_torch_unavailable
                : cameraController.isTorchOn() ? R.string.camera_torch_off : R.string.camera_torch_on);
        btnTestRecord.setEnabled(ready);
        btnTestRecord.setText(recording ? R.string.monitor_stop_test_record : R.string.monitor_test_record);
        if (ready) {
            String cameraName = getString(cameraController.getLensFacing() == CameraSelector.LENS_FACING_FRONT
                    ? R.string.camera_front : R.string.camera_back);
            cameraStatus.setText(recording ? getString(R.string.camera_recording_status, cameraName) : cameraName);
        }
    }

    private void stopRecording() {
        handler.removeCallbacks(stopTestRecording);
        appController.stopRecording(new RecordingController.Callback() {
            @Override
            public void onRecordingSaved(@NonNull String filePath) {
                if (getView() == null) return;
                showMessage(getString(R.string.recording_saved, filePath));
                refreshControls();
            }

            @Override
            public void onRecordingFailed(@NonNull String reason) {
                if (getView() == null) return;
                showMessage(getString(R.string.recording_failed, reason));
                refreshControls();
            }
        });
    }

    private void showMessage(String message) {
        if (getView() != null) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraReady() {
        refreshControls();
    }

    @Override
    public void onCameraError(int messageId) {
        if (cameraStatus == null) return;
        if (cameraController.isReady()) {
            showMessage(getString(messageId));
            return;
        }
        cameraStatus.setText(messageId);
        btnCameraPermission.setText(R.string.camera_retry);
        btnCameraPermission.setVisibility(View.VISIBLE);
        refreshControls();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCamera();
        handler.post(updateStatus);
    }

    private void stopCamera() {
        handler.removeCallbacks(updateStatus);
        handler.removeCallbacks(stopTestRecording);
        cameraController.stop();
    }

    @Override
    public void onPause() {
        stopCamera();
        super.onPause();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (cameraController == null) return;
        if (hidden) stopCamera();
        else if (isResumed()) {
            refreshCamera();
            handler.removeCallbacks(updateStatus);
            handler.post(updateStatus);
        }
    }

    @Override
    public void onDestroyView() {
        stopCamera();
        statusChip = null;
        cameraStatus = null;
        previewView = null;
        btnCameraPermission = null;
        btnCameraSettings = null;
        btnSwitchCamera = null;
        btnTorch = null;
        btnTestRecord = null;
        super.onDestroyView();
    }
}
