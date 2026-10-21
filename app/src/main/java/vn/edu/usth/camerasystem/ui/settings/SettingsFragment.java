package vn.edu.usth.camerasystem.ui.settings;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import vn.edu.usth.camerasystem.R;
import vn.edu.usth.camerasystem.alert.AlertManager;
import vn.edu.usth.camerasystem.ui.AppController;

/** Màn hình cài đặt chế độ giám sát, độ nhạy và mã PIN. */
public class SettingsFragment extends Fragment {

    private static final String PREFS = "camera_security_prefs";
    private static final String KEY_PIN = "pin";

    private AlertManager alertManager;
    private TextView sensitivityLabel;
    private TextView pinStatus;
    private EditText etPin;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(), granted -> {
                        if (!granted && isAdded()) {
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
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        alertManager = AppController.get().getAlertManager();

        SwitchMaterial switchArmed = view.findViewById(R.id.switch_armed);
        switchArmed.setChecked(alertManager.isArmed());
        switchArmed.setOnCheckedChangeListener((buttonView, isChecked) -> {
            alertManager.setArmed(isChecked);
            if (isChecked) {
                requestNotificationPermissionIfNeeded();
            }
        });

        SeekBar seekSensitivity = view.findViewById(R.id.seek_sensitivity);
        sensitivityLabel = view.findViewById(R.id.sensitivity_label);

        int sensitivity = alertManager.getSensitivity();
        seekSensitivity.setProgress(sensitivity);
        updateSensitivityLabel(sensitivity);
        seekSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateSensitivityLabel(progress);
                if (fromUser) {
                    alertManager.setSensitivity(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        etPin = view.findViewById(R.id.et_pin);
        pinStatus = view.findViewById(R.id.tv_pin_status);
        MaterialButton btnSavePin = view.findViewById(R.id.btn_save_pin);

        refreshPinStatus();

        btnSavePin.setOnClickListener(v -> savePin());
    }

    private SharedPreferences getPrefs() {
        return requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private void savePin() {
        String pin = etPin.getText().toString().trim();

        if (pin.isEmpty()) {
            getPrefs().edit().remove(KEY_PIN).apply();
            Toast.makeText(requireContext(), R.string.pin_removed, Toast.LENGTH_SHORT).show();
            etPin.setText("");
            refreshPinStatus();
            return;
        }

        if (pin.length() < 4 || pin.length() > 6) {
            Toast.makeText(requireContext(), R.string.pin_error_length, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pin.matches("\\d+")) {
            Toast.makeText(requireContext(), R.string.pin_error_digits, Toast.LENGTH_SHORT).show();
            return;
        }

        getPrefs().edit().putString(KEY_PIN, pin).apply();
        Toast.makeText(requireContext(), R.string.pin_saved, Toast.LENGTH_SHORT).show();
        etPin.setText("");
        refreshPinStatus();
    }

    private void refreshPinStatus() {
        String saved = getPrefs().getString(KEY_PIN, null);
        if (saved != null && !saved.isEmpty()) {
            pinStatus.setText(getString(R.string.pin_status_set, saved.length()));
        } else {
            pinStatus.setText(R.string.pin_status_not_set);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void updateSensitivityLabel(int value) {
        sensitivityLabel.setText(getString(R.string.settings_sensitivity_label, value));
    }
}
