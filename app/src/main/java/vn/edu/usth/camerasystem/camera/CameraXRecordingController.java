package vn.edu.usth.camerasystem.camera;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;

/** Ghi video CameraX vào thư mục riêng của ứng dụng; P2 truyền VideoCapture sau khi bind camera. */
public final class CameraXRecordingController implements RecordingController {

    private static final String TAG = "GhiVideo";
    private static final String THU_MUC_VIDEO = "Videos";

    private final Context context;
    private final Executor luongChinh;
    private VideoCapture<androidx.camera.video.Recorder> boGhiCamera;
    private Recording phienGhi;
    private File tepVideo;
    private long batDauLuc;
    private Callback callbackDangCho;

    public CameraXRecordingController(@NonNull Context context, @NonNull Executor luongChinh) {
        this.context = context.getApplicationContext();
        this.luongChinh = luongChinh;
    }

    /** P2 gọi sau khi tạo use case VideoCapture và bind vào lifecycle. */
    public synchronized void ganBoGhi(@Nullable VideoCapture<androidx.camera.video.Recorder> boGhi) {
        if (phienGhi == null) {
            boGhiCamera = boGhi;
        }
    }

    @Override
    public synchronized boolean isRecording() {
        return phienGhi != null;
    }

    @Override
    public synchronized boolean startRecording() {
        if (phienGhi != null) {
            Log.d(TAG, "Bỏ qua yêu cầu: video đang được ghi");
            return false;
        }
        if (boGhiCamera == null) {
            Log.w(TAG, "Chưa có CameraX VideoCapture; P2 cần gọi ganBoGhi()");
            return false;
        }

        File thuMuc = context.getExternalFilesDir(THU_MUC_VIDEO);
        if (thuMuc == null || (!thuMuc.exists() && !thuMuc.mkdirs())) {
            Log.e(TAG, "Không tạo được thư mục lưu video");
            return false;
        }
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.e(TAG, "Bộ nhớ ngoài của ứng dụng không sẵn sàng");
            return false;
        }

        String mocThoiGian = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        tepVideo = new File(thuMuc, "event_" + mocThoiGian + ".mp4");
        batDauLuc = System.currentTimeMillis();
        FileOutputOptions tuyChonDauRa = new FileOutputOptions.Builder(tepVideo).build();

        try {
            phienGhi = boGhiCamera.getOutput()
                    .prepareRecording(context, tuyChonDauRa)
                    .start(luongChinh, this::xuLySuKienGhi);
            Log.i(TAG, "Bắt đầu ghi: " + tepVideo.getAbsolutePath());
            return true;
        } catch (RuntimeException loi) {
            phienGhi = null;
            tepVideo.delete();
            tepVideo = null;
            Log.e(TAG, "Không bắt đầu được video", loi);
            return false;
        }
    }

    @Override
    public synchronized void stopRecording(@Nullable Callback callback) {
        if (phienGhi == null) {
            if (callback != null) callback.onRecordingFailed("Hiện không có video đang ghi");
            return;
        }
        callbackDangCho = callback;
        phienGhi.stop();
    }

    private void xuLySuKienGhi(@NonNull VideoRecordEvent suKien) {
        if (!(suKien instanceof VideoRecordEvent.Finalize)) return;

        VideoRecordEvent.Finalize ketThuc = (VideoRecordEvent.Finalize) suKien;
        Callback callback;
        File tep;
        long batDau;
        synchronized (this) {
            callback = callbackDangCho;
            tep = tepVideo;
            batDau = batDauLuc;
            callbackDangCho = null;
            phienGhi = null;
            tepVideo = null;
        }

        if (ketThuc.hasError() || tep == null || !tep.exists() || tep.length() == 0) {
            if (tep != null) tep.delete();
            String loi = "CameraX không hoàn tất được video (mã " + ketThuc.getError() + ")";
            Log.e(TAG, loi);
            if (callback != null) callback.onRecordingFailed(loi);
            return;
        }

        long ketThucLuc = System.currentTimeMillis();
        try {
            luuThongTin(tep, batDau, ketThucLuc);
            Log.i(TAG, "Đã lưu video: " + tep.getAbsolutePath());
            if (callback != null) callback.onRecordingSaved(tep.getAbsolutePath());
        } catch (IOException loi) {
            Log.e(TAG, "Video đã lưu nhưng không lưu được metadata", loi);
            if (callback != null) callback.onRecordingFailed("Video có nhưng metadata không lưu được: " + loi.getMessage());
        }
    }

    private void luuThongTin(@NonNull File tep, long batDau, long ketThuc) throws IOException {
        File tepThongTin = new File(tep.getParentFile(), tep.getName().replace(".mp4", ".json"));
        try (FileWriter writer = new FileWriter(tepThongTin)) {
            writer.write("{\n");
            writer.write("  \"tep_video\": \"" + tep.getName() + "\",\n");
            writer.write("  \"bat_dau_ms\": " + batDau + ",\n");
            writer.write("  \"ket_thuc_ms\": " + ketThuc + ",\n");
            writer.write("  \"thoi_luong_ms\": " + Math.max(0, ketThuc - batDau) + ",\n");
            writer.write("  \"kich_thuoc_byte\": " + tep.length() + "\n");
            writer.write("}\n");
        }
    }
}
