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
 * Settings tab — nối AlertManager (Armed + sensitivity). PIN do P6 bổ sung.
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

        SwitchMaterial switchArmed = view.findViewById(R.id.switch_armed);
        SeekBar seekSensitivity = view.findViewById(R.id.seek_sensitivity);
        sensitivityLabel = view.findViewById(R.id.sensitivity_label);

        switchArmed.setChecked(alertManager.isArmed());
        switchArmed.setOnCheckedChangeListener((buttonView, isChecked) ->
                alertManager.setArmed(isChecked));

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
    }

    private void updateSensitivityLabel(int value) {
        sensitivityLabel.setText(getString(R.string.settings_sensitivity_label, value));
    }
}
