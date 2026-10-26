package vn.edu.usth.camerasystem.motion;

import java.nio.ByteBuffer;

public final class MotionDetector {

    // Chia ảnh thành lưới 32 x 24 ô, mỗi ô lấy độ sáng trung bình
    private static final int SO_COT = 32;
    private static final int SO_HANG = 24;
    private static final int BUOC_LAY_MAU = 4;

    // Nếu hơn 90% ô cùng đổi thì coi là đổi ánh sáng cả khung, không tính là chuyển động
    private static final float NGUONG_DOI_SANG = 0.9f;

    // Độ nhạy 0..100 (100 là nhạy nhất)
    private static final float DIEN_TICH_NHAY_THAP = 0.20f; // cần 20% ảnh thay đổi
    private static final float DIEN_TICH_NHAY_CAO = 0.01f;  // chỉ cần 1% ảnh thay đổi
    private static final int CHENH_SANG_NHAY_THAP = 40;
    private static final int CHENH_SANG_NHAY_CAO = 15;

    private int[] anhHienTai = new int[SO_COT * SO_HANG];
    private int[] anhTruoc = new int[SO_COT * SO_HANG];
    private boolean coAnhTruoc;
    private float diem;

    public void datLai() {
        coAnhTruoc = false;
    }

    /** Điểm của lần phân tích gần nhất: tỉ lệ ô thay đổi, từ 0 đến 1. */
    public float getDiem() {
        return diem;
    }

    public boolean phatHien(ByteBuffer anh, int rong, int cao, int buocHang, int buocDiem, int doNhay) {
        layMau(anh, rong, cao, buocHang, buocDiem);

        boolean coChuyenDong = false;
        diem = 0;
        if (coAnhTruoc) {
            diem = tinhDiem(doNhay);
            coChuyenDong = diem >= nguongDienTich(doNhay) && diem < NGUONG_DOI_SANG;
        }

        int[] tam = anhTruoc;
        anhTruoc = anhHienTai;
        anhHienTai = tam;
        coAnhTruoc = true;
        return coChuyenDong;
    }

    private float tinhDiem(int doNhay) {
        int nguongSang = nguongSang(doNhay);
        int soODoi = 0;
        for (int i = 0; i < anhHienTai.length; i++) {
            if (Math.abs(anhHienTai[i] - anhTruoc[i]) >= nguongSang) {
                soODoi++;
            }
        }
        return soODoi / (float) anhHienTai.length;
    }

    private void layMau(ByteBuffer anh, int rong, int cao, int buocHang, int buocDiem) {
        int rongO = rong / SO_COT;
        int caoO = cao / SO_HANG;
        for (int hang = 0; hang < SO_HANG; hang++) {
            for (int cot = 0; cot < SO_COT; cot++) {
                int tong = 0;
                int soDiem = 0;
                for (int y = hang * caoO; y < (hang + 1) * caoO; y += BUOC_LAY_MAU) {
                    for (int x = cot * rongO; x < (cot + 1) * rongO; x += BUOC_LAY_MAU) {
                        tong += anh.get(y * buocHang + x * buocDiem) & 0xFF;
                        soDiem++;
                    }
                }
                anhHienTai[hang * SO_COT + cot] = tong / soDiem;
            }
        }
    }

    /** Tỉ lệ ô phải đổi để tính là chuyển động: 0.20 (độ nhạy 0) giảm dần về 0.01 (độ nhạy 100). */
    public static float nguongDienTich(int doNhay) {
        double ti = DIEN_TICH_NHAY_CAO / DIEN_TICH_NHAY_THAP;
        return DIEN_TICH_NHAY_THAP * (float) Math.pow(ti, doNhay / 100.0);
    }

    /** Độ lệch sáng tối thiểu để một ô được tính là đổi: 40 (độ nhạy 0) giảm dần về 15 (độ nhạy 100). */
    public static int nguongSang(int doNhay) {
        return CHENH_SANG_NHAY_THAP
                + (CHENH_SANG_NHAY_CAO - CHENH_SANG_NHAY_THAP) * doNhay / 100;
    }
}