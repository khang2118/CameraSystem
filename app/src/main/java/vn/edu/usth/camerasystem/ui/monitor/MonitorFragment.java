package vn.edu.usth.camerasystem.ui.monitor;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.Recorder;
import androidx.camera.video.VideoCapture;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;

import vn.edu.usth.camerasystem.R;
import vn.edu.usth.camerasystem.alert.AlertManager;
import vn.edu.usth.camerasystem.motion.MotionAnalyzer;
import vn.edu.usth.camerasystem.ui.AppController;

/**
 * Monitor tab — live preview shell. CameraX sẽ do P2 gắn vào.
 * Nút giả lập motion dùng để test interface Day 3 (trước khi có P4).
 */
public class MonitorFragment extends Fragment {

    private TextView statusChip;
    private AlertManager alertManager;
    private androidx.camera.view.PreviewView xemTruoc;
    private ProcessCameraProvider nhaCungCapCamera;
    private MaterialButton nutGhiThu;
    private final Handler boHenGio = new Handler(Looper.getMainLooper());
    private final Runnable tuDungGhi = () -> dungGhiThu();
    private final ActivityResultLauncher<String> xinQuyenCamera = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), duocCap -> {
                if (duocCap) batCamera();
                else thongBao(getString(R.string.camera_permission_denied));
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
        xemTruoc = view.findViewById(R.id.camera_preview);
        nutGhiThu = view.findViewById(R.id.btn_test_record);

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

        nutGhiThu.setOnClickListener(v -> {
            if (AppController.get().getRecordingController().isRecording()) {
                dungGhiThu();
            } else if (AppController.get().getRecordingController().startRecording()) {
                nutGhiThu.setText(R.string.monitor_stop_test_record);
                thongBao(getString(R.string.recording_started));
                boHenGio.postDelayed(tuDungGhi, 5000);
            } else {
                thongBao(getString(R.string.recording_start_failed));
            }
        });

        refreshArmedUi();
        kiemTraQuyenVaBatCamera();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (alertManager != null) {
            refreshArmedUi();
        }
    }

    @Override
    public void onDestroyView() {
        boHenGio.removeCallbacks(tuDungGhi);
        if (AppController.get().getRecordingController().isRecording()) dungGhiThu();
        if (nhaCungCapCamera != null) {
            nhaCungCapCamera.unbindAll();
            nhaCungCapCamera = null;
        }
        AppController.get().getCameraXRecordingController().ganBoGhi(null);
        xemTruoc = null;
        nutGhiThu = null;
        super.onDestroyView();
    }

    private void kiemTraQuyenVaBatCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            batCamera();
        } else {
            xinQuyenCamera.launch(android.Manifest.permission.CAMERA);
        }
    }

    private void batCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            if (!isAdded() || xemTruoc == null) return;
            try {
                nhaCungCapCamera = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(xemTruoc.getSurfaceProvider());
                Recorder recorder = new Recorder.Builder().build();
                VideoCapture<Recorder> videoCapture = VideoCapture.withOutput(recorder);
                ImageAnalysis phanTich = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                phanTich.setAnalyzer(ContextCompat.getMainExecutor(requireContext()),
                        new MotionAnalyzer(AppController.get(), alertManager,
                                AppController.get().getRecordingController()));
                nhaCungCapCamera.unbindAll();
                nhaCungCapCamera.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA,
                        preview, videoCapture, phanTich);
                AppController.get().getCameraXRecordingController().ganBoGhi(videoCapture);
            } catch (Exception loi) {
                thongBao(getString(R.string.camera_start_failed, loi.getMessage()));
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void dungGhiThu() {
        boHenGio.removeCallbacks(tuDungGhi);
        AppController.get().getRecordingController().stopRecording(
                new vn.edu.usth.camerasystem.camera.RecordingController.Callback() {
                    @Override
                    public void onRecordingSaved(@NonNull String filePath) {
                        if (nutGhiThu != null) nutGhiThu.setText(R.string.monitor_test_record);
                        thongBao(getString(R.string.recording_saved, filePath));
                    }

                    @Override
                    public void onRecordingFailed(@NonNull String reason) {
                        if (nutGhiThu != null) nutGhiThu.setText(R.string.monitor_test_record);
                        thongBao(getString(R.string.recording_failed, reason));
                    }
                });
        if (nutGhiThu != null) nutGhiThu.setText(R.string.monitor_test_record);
    }

    private void thongBao(@NonNull String noiDung) {
        if (isAdded()) Toast.makeText(requireContext(), noiDung, Toast.LENGTH_LONG).show();
    }

    private void refreshArmedUi() {
        boolean armed = alertManager.isArmed();
        statusChip.setText(armed ? R.string.monitor_status_armed : R.string.monitor_status_disarmed);
        statusChip.setBackgroundResource(
                armed ? R.drawable.bg_status_chip_armed : R.drawable.bg_status_chip
        );
    }
}