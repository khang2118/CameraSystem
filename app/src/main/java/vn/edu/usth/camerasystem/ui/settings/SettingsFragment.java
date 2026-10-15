package vn.edu.usth.camerasystem.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import vn.edu.usth.camerasystem.R;
import vn.edu.usth.camerasystem.alert.AlertManager;
import vn.edu.usth.camerasystem.ui.AppController;

/**
 * Settings tab — nối AlertManager (Armed + sensitivity).
 * Day 2: UI cơ bản với Switch Armed, SeekBar độ nhạy, ô PIN (chưa có logic PIN).
 * P6 sẽ bổ sung logic PIN từ Day 3.
 */
public class SettingsFragment extends Fragment {

    private AlertManager alertManager;
    private TextView sensitivityLabel;

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

        // ===== Armed switch =====
        SwitchMaterial switchArmed = view.findViewById(R.id.switch_armed);
        switchArmed.setChecked(alertManager.isArmed());
        switchArmed.setOnCheckedChangeListener((buttonView, isChecked) ->
                alertManager.setArmed(isChecked));

        // ===== Sensitivity SeekBar =====
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

        // ===== PIN =====
        // TODO Day 3: đọc/ghi PIN từ SharedPreferences
        // UI ô PIN đã có trong layout, logic sẽ bổ sung sau.
    }

    private void updateSensitivityLabel(int value) {
        sensitivityLabel.setText(getString(R.string.settings_sensitivity_label, value));
    }
}
