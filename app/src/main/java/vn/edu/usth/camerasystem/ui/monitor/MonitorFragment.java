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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import vn.edu.usth.camerasystem.R;
import vn.edu.usth.camerasystem.alert.AlertManager;
import vn.edu.usth.camerasystem.ui.AppController;

/** P2 camera permission and the existing P1 Armed/motion test controls. */
public class MonitorFragment extends Fragment {

    private TextView statusChip;
    private TextView cameraStatus;
    private MaterialButton btnCameraPermission;
    private AlertManager alertManager;

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
        statusChip = view.findViewById(R.id.status_chip);
        cameraStatus = view.findViewById(R.id.camera_status);
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
        if (cameraStatus == null) return;
        boolean granted = hasCameraPermission();
        cameraStatus.setText(granted
                ? R.string.camera_permission_granted : R.string.camera_permission_needed);
        btnCameraPermission.setVisibility(granted ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshArmedUi();
        refreshCamera();
    }

    private void refreshArmedUi() {
        boolean armed = alertManager.isArmed();
        statusChip.setText(armed ? R.string.monitor_status_armed : R.string.monitor_status_disarmed);
        statusChip.setBackgroundResource(
                armed ? R.drawable.bg_status_chip_armed : R.drawable.bg_status_chip);
    }
}
