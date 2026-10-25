package vn.edu.usth.camerasystem.motion;

import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import vn.edu.usth.camerasystem.alert.AlertManager;
import vn.edu.usth.camerasystem.camera.RecordingController;

public final class MotionAnalyzer implements ImageAnalysis.Analyzer {

    private static final String TAG = "MotionAnalyzer";

    private static final long KHOANG_CACH_MS = 150;       // 2 lần phân tích cách nhau tối thiểu
    private static final long CHO_ON_DINH_MS = 1500;      // chờ camera ổn định sau khi bật Armed
    private static final int SO_KHUNG_LIEN_TIEP = 2;      // cần 2 lần liên tiếp có chuyển động
    private static final long THOI_GIAN_NGHI_MS = 10_000; // sau mỗi lần báo, nghỉ 10 giây

    private final MotionDetector boPhatHien = new MotionDetector();
    private final MotionListener nguoiNghe;
    private final AlertManager alertManager;
    private final RecordingController recordingController;

    private long lanPhanTichCuoi;
    private long batDauLuc;
    private long lanBaoCuoi = -THOI_GIAN_NGHI_MS;
    private int soKhungLienTiep;

    public MotionAnalyzer(@NonNull MotionListener nguoiNghe,
                          @NonNull AlertManager alertManager,
                          @NonNull RecordingController recordingController) {
        this.nguoiNghe = nguoiNghe;
        this.alertManager = alertManager;
        this.recordingController = recordingController;
    }

    @Override
    public void analyze(@NonNull ImageProxy anh) {
        try {
            xuLy(anh);
        } finally {
            anh.close();
        }
    }

    private void xuLy(ImageProxy anh) {
        long bayGio = SystemClock.elapsedRealtime();

        if (!alertManager.isArmed() || recordingController.isRecording()) {
            boPhatHien.datLai();
            batDauLuc = 0;
            soKhungLienTiep = 0;
            return;
        }
        if (batDauLuc == 0) {
            batDauLuc = bayGio;
        }
        if (bayGio - lanPhanTichCuoi < KHOANG_CACH_MS) {
            return;
        }
        lanPhanTichCuoi = bayGio;

        ImageProxy.PlaneProxy matY = anh.getPlanes()[0];
        boolean coChuyenDong = boPhatHien.phatHien(matY.getBuffer(),
                anh.getWidth(), anh.getHeight(),
                matY.getRowStride(), matY.getPixelStride(),
                alertManager.getSensitivity());
        Log.d(TAG, "diem=" + boPhatHien.getDiem() + " coChuyenDong=" + coChuyenDong);

        if (bayGio - batDauLuc < CHO_ON_DINH_MS) {
            return;
        }
        soKhungLienTiep = coChuyenDong ? soKhungLienTiep + 1 : 0;
        if (soKhungLienTiep < SO_KHUNG_LIEN_TIEP) {
            return;
        }
        soKhungLienTiep = 0;

        if (bayGio - lanBaoCuoi < THOI_GIAN_NGHI_MS) {
            return;
        }
        lanBaoCuoi = bayGio;

        Log.i(TAG, "Phát hiện chuyển động, điểm=" + boPhatHien.getDiem());
        nguoiNghe.onMotionDetected(boPhatHien.getDiem(), System.currentTimeMillis());
    }
}