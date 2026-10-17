package vn.edu.usth.camerasystem.ui.monitor;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
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

/** Màn hình theo dõi, trạng thái Armed và nút thử cảnh báo. */
public class MonitorFragment extends Fragment {

    private TextView statusChip;
    private AlertManager alertManager;

    private final ActivityResultLauncher<String> notifPermLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (!granted) {
                            Toast.makeText(requireContext(),
                                    R.string.notification_permission_required,
                                    Toast.LENGTH_LONG).show();
                        }
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

        MaterialButton btnToggleArm = view.findViewById(R.id.btn_toggle_arm);
        MaterialButton btnSimulate = view.findViewById(R.id.btn_simulate_motion);

        btnToggleArm.setOnClickListener(v -> {
            boolean next = !alertManager.isArmed();
            alertManager.setArmed(next);
            refreshArmedUi();
            Toast.makeText(
                    requireContext(),
                    next ? R.string.monitor_status_armed : R.string.monitor_status_disarmed,
                    Toast.LENGTH_SHORT
            ).show();

            if (next) {
                requestNotificationPermissionIfNeeded();
            }
        });

        btnSimulate.setOnClickListener(v -> {
            if (!alertManager.isArmed()) {
                Toast.makeText(requireContext(), R.string.monitor_status_disarmed, Toast.LENGTH_SHORT).show();
                return;
            }
            float score = alertManager.getSensitivity() / 100f;
            AppController.get().onMotionDetected(score, System.currentTimeMillis());
            Toast.makeText(requireContext(), R.string.monitor_simulate_motion, Toast.LENGTH_SHORT).show();
        });

        refreshArmedUi();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (alertManager != null) {
            refreshArmedUi();
        }
    }

    private void refreshArmedUi() {
        boolean armed = alertManager.isArmed();
        statusChip.setText(armed ? R.string.monitor_status_armed : R.string.monitor_status_disarmed);
        statusChip.setBackgroundResource(
                armed ? R.drawable.bg_status_chip_armed : R.drawable.bg_status_chip
        );
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }
}
