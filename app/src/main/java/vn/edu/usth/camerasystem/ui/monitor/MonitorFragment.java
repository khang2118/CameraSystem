package vn.edu.usth.camerasystem.ui.monitor;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import vn.edu.usth.camerasystem.R;
import vn.edu.usth.camerasystem.alert.AlertManager;
import vn.edu.usth.camerasystem.camera.CameraController;
import vn.edu.usth.camerasystem.ui.AppController;

/** P2 camera preview and the existing P1 Armed/motion test controls. */
public class MonitorFragment extends Fragment implements CameraController.Listener {

    private TextView statusChip;
    private TextView cameraStatus;
    private PreviewView previewView;
    private MaterialButton btnCameraPermission;
    private AlertManager alertManager;
    private CameraController cameraController;

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
        alertManager = AppController.get().getAlertManager();
        cameraController = AppController.get().getCameraController();
        statusChip = view.findViewById(R.id.status_chip);
        cameraStatus = view.findViewById(R.id.camera_status);
        previewView = view.findViewById(R.id.preview_view);
        btnCameraPermission = view.findViewById(R.id.btn_camera_permission);

        btnCameraPermission.setOnClickListener(v -> {
            if (hasCameraPermission()) refreshCamera();
            else cameraPermission.launch(Manifest.permission.CAMERA);
        });

        MaterialButton btnToggleArm = view.findViewById(R.id.btn_toggle_arm);
        MaterialButton btnSimulate = view.findViewById(R.id.btn_simulate_motion);
        btnToggleArm.setOnClickListener(v -> {
            boolean next = !alertManager.isArmed();
            alertManager.setArmed(next);
            refreshArmedUi();
            Toast.makeText(requireContext(),
                    next ? R.string.monitor_status_armed : R.string.monitor_status_disarmed,
                    Toast.LENGTH_SHORT).show();
        });
        btnSimulate.setOnClickListener(v -> {
            if (!alertManager.isArmed()) {
                Toast.makeText(requireContext(), R.string.monitor_status_disarmed,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            float score = alertManager.getSensitivity() / 100f;
            AppController.get().onMotionDetected(score, System.currentTimeMillis());
            Toast.makeText(requireContext(), R.string.monitor_simulate_motion,
                    Toast.LENGTH_SHORT).show();
        });
        refreshArmedUi();
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void refreshCamera() {
        if (previewView == null || isHidden()) return;
        boolean granted = hasCameraPermission();
        btnCameraPermission.setText(R.string.camera_permission_grant);
        btnCameraPermission.setVisibility(granted ? View.GONE : View.VISIBLE);
        if (!granted) {
            cameraController.stop();
            cameraStatus.setText(R.string.camera_permission_needed);
            return;
        }
        cameraStatus.setText(R.string.camera_loading);
        cameraController.start(getViewLifecycleOwner(), previewView, this);
    }

    @Override
    public void onCameraReady() {
        if (cameraStatus == null) return;
        cameraStatus.setText(R.string.camera_ready);
    }

    @Override
    public void onCameraError(int messageId) {
        if (cameraStatus == null) return;
        cameraStatus.setText(messageId);
        btnCameraPermission.setText(R.string.camera_retry);
        btnCameraPermission.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshArmedUi();
        refreshCamera();
    }

    @Override
    public void onPause() {
        cameraController.stop();
        super.onPause();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (cameraController == null) return;
        if (hidden) cameraController.stop();
        else if (isResumed()) refreshCamera();
    }

    @Override
    public void onDestroyView() {
        cameraController.stop();
        statusChip = null;
        cameraStatus = null;
        previewView = null;
        btnCameraPermission = null;
        super.onDestroyView();
    }

    private void refreshArmedUi() {
        boolean armed = alertManager.isArmed();
        statusChip.setText(armed ? R.string.monitor_status_armed : R.string.monitor_status_disarmed);
        statusChip.setBackgroundResource(
                armed ? R.drawable.bg_status_chip_armed : R.drawable.bg_status_chip);
    }
}
