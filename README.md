# CameraSystem — Camera Security System

Ứng dụng Android giám sát an ninh đơn giản: dùng điện thoại như camera bảo vệ, phát hiện chuyển động, tự ghi clip ngắn, lưu sự kiện và gửi thông báo.

> Đồ án môn lập trình ứng dụng di động (trình độ sinh viên).  
> Repo: [https://github.com/khang2118/CameraSystem](https://github.com/khang2118/CameraSystem)

---

## Ý tưởng dự án

Dùng điện thoại như **camera giám sát**:

1. Bật chế độ **Armed**
2. Xem **live preview** từ camera
3. Khi có **chuyển động** → tự ghi video ngắn
4. Lưu vào **Events** + gửi **Notification**
5. Xem lại / xóa sự kiện; cấu hình độ nhạy & PIN trong **Settings**

### MVP

| Tính năng | Mô tả |
|-----------|--------|
| Live preview | CameraX preview (trước/sau) |
| Armed / Disarmed | Bật/tắt giám sát |
| Motion detect | So sánh frame đơn giản + cooldown |
| Auto record | Ghi 5–10 giây khi có chuyển động |
| Events | Danh sách sự kiện, xem/xóa clip |
| Alert | Notification khi có sự kiện |
| Settings | Độ nhạy, PIN khóa app |

### Không làm trong phạm vi 14 ngày

- Cloud / AI nhận diện người
- Camera IP nhiều thiết bị
- Streaming qua Internet công cộng

---

## Phân vai (6 người)

| Người | Vai | Chịu trách nhiệm chính |
|-------|-----|------------------------|
| **P1** | Team Lead + UI shell | Repo, kiến trúc, navigation, merge, demo — branch `khang-ui` |
| **P2** | Camera | CameraX preview, đổi cam, flash, lifecycle |
| **P3** | Recording | Ghi/dừng video, lưu file, metadata |
| **P4** | Motion detect | So sánh frame, độ nhạy, trigger sự kiện |
| **P5** | Events + Storage | DB/list sự kiện, xem lại clip, xóa |
| **P6** | Settings + Alert + PIN | Cài đặt, notification, Armed/Disarmed, PIN |

### Package gợi ý

```
vn.edu.usth.camerasystem/
├── ui/          # MainActivity, Fragments
├── camera/      # CameraX preview / analysis
├── motion/      # MotionDetector
├── storage/     # Event model, Room/SQLite
└── alert/       # Notification, PIN helpers
```

### Branch gợi ý

- `khang-ui` (P1)
- `feat/p2-camera`
- `feat/p3-record`
- `feat/p4-motion`
- `feat/p5-events`
- `feat/p6-settings-alert`

---

## Timeline tổng quan (14 ngày)

| Giai đoạn | Ngày | Mục tiêu |
|-----------|------|----------|
| Phase 0 | Day 1 | Setup, chia branch, chốt UI wireframe |
| Phase 1 | Day 2–5 | Xây từng module riêng, ghép được preview |
| Phase 2 | Day 6–9 | Nối motion → record → event → notify |
| Phase 3 | Day 10–12 | Polish UI, bug, quyền, PIN |
| Phase 4 | Day 13–14 | Test end-to-end, demo, slide/README |

### Cột mốc bắt buộc

| Ngày | Mốc |
|------|-----|
| **D3** | Module riêng chạy được |
| **D5** | Interface chốt, sẵn sàng ghép |
| **D7** | Full pipeline chạy 1 vòng |
| **D9** | Feature gần đủ |
| **D12** | Test xong, bug lớn hết |
| **D14** | Demo + nộp |

---

## Bảng chia việc chi tiết từng người × từng ngày

### Day 1 — Kickoff & setup

| Người | Việc cần làm (DONE khi…) |
|-------|---------------------------|
| **P1** | Tạo package `ui/camera/motion/storage/alert`; bottom nav 3 tab trống; README ngắn quy ước branch/merge. **Done:** app mở được 3 màn trống. |
| **P2** | Nghiên cứu CameraX Preview; liệt kê dependency cần thêm vào `libs.versions.toml`. **Done:** list dependency + note máy test. |
| **P3** | Nghiên cứu CameraX VideoCapture; quyết định nơi lưu file (`getExternalFilesDir`). **Done:** note path + API dự kiến. |
| **P4** | Prototype so sánh 2 ảnh (luminance). **Done:** hàm demo nhận 2 Bitmap → true/false. |
| **P5** | Thiết kế model `Event` (id, timestamp, videoPath, thumbnailPath, note). **Done:** schema chốt với nhóm. |
| **P6** | Wireframe Settings + list key SharedPreferences (`armed`, `sensitivity`, `pin`). **Done:** mock UI XML hoặc sketch. |

---

### Day 2 — Nền tảng

| Người | Việc cần làm |
|-------|----------------|
| **P1** | Tạo `MainActivity` + 3 Fragment (`Monitor`, `Events`, `Settings`); theme/colors cơ bản. |
| **P2** | Thêm CameraX deps; xin quyền `CAMERA` trong Manifest + runtime. |
| **P3** | Xin quyền `RECORD_AUDIO` (nếu ghi tiếng); tạo folder lưu video. |
| **P4** | Viết class `MotionDetector` (input: byte/Bitmap, output: score + isMotion). |
| **P5** | Setup Room/SQLite: `EventDao`, `EventDatabase`. |
| **P6** | Tạo `SettingsFragment` UI: SeekBar độ nhạy, Switch Armed, ô PIN. |

---

### Day 3 — Module chạy riêng

| Người | Việc cần làm |
|-------|----------------|
| **P1** | Định nghĩa interface chung: `RecordingController`, `MotionListener`, `EventRepository`, `AlertManager`. |
| **P2** | Preview hiển thị trên `MonitorFragment` (máy thật). |
| **P3** | Nút Record thủ công: start 5s → stop → log path file. |
| **P4** | Manual test: thay đổi threshold → kết quả motion đổi. |
| **P5** | Insert 3 event giả → hiển thị RecyclerView. |
| **P6** | Đọc/ghi SharedPreferences; Armed switch lưu được qua restart app. |

**Milestone Day 3:** mỗi người demo được phần của mình trên branch.

---

### Day 4 — Mở rộng module

| Người | Việc cần làm |
|-------|----------------|
| **P1** | ViewModel/`AppController` điều phối Arm state; merge sớm code P2 vào main nếu ổn. |
| **P2** | Xử lý lifecycle: pause/stop camera khi rời màn; không leak. |
| **P3** | API `startRecording()` / `stopRecording(callback path)`; chống double-start. |
| **P4** | Gắn `ImageAnalysis` (phối hợp P2) — nhận frame liên tục. |
| **P5** | Adapter Events: hiện giờ + icon; click item (chưa cần play). |
| **P6** | `NotificationHelper`: tạo channel, hàm `showMotionAlert(title, body)`. |

---

### Day 5 — Sẵn sàng ghép

| Người | Việc cần làm |
|-------|----------------|
| **P1** | Checklist merge Day 6; fix conflict navigation/permissions. |
| **P2** | Nút đổi camera trước/sau trên Monitor. |
| **P3** | Đặt tên file `event_yyyyMMdd_HHmmss.mp4`; test ghi 3 lần liên tiếp. |
| **P4** | Thêm cooldown 10s sau mỗi lần detect; log rõ. |
| **P5** | Hàm `addEvent(path, time)` dùng được từ module khác. |
| **P6** | Nút Armed/Disarmed trên Monitor (bind Prefs); Disarmed → không alert. |

**Milestone Day 5:** interface + module độc lập xong.

---

### Day 6 — Ghép luồng A (Camera + Motion + Arm)

| Người | Việc cần làm |
|-------|----------------|
| **P1** | Merge P2+P4; điều phối session ghép; ghi bug list. |
| **P2** | Preview + ImageAnalysis chạy song song ổn định. |
| **P3** | Standby: chuẩn bị hook nhận tín hiệu từ P4. |
| **P4** | Khi Armed + motion → gọi `onMotionDetected()`. |
| **P5** | Quan sát API; chuẩn bị insert khi có path thật. |
| **P6** | Chỉ cho phép detect/alert khi `armed == true`. |

**Done Day 6:** vẫy tay trước cam → motion callback chạy (chưa bắt buộc đã ghi video).

---

### Day 7 — Ghép luồng B (Record + Event + Notify)

| Người | Việc cần làm |
|-------|----------------|
| **P1** | Nối full pipeline trên main; demo nhóm cuối ngày. |
| **P2** | Fix crash khi vừa analyze vừa record (nếu có). |
| **P3** | `onMotionDetected` → auto record 5–10s → trả path. |
| **P4** | Trong lúc đang record: tạm bỏ qua motion mới (tránh spam). |
| **P5** | Nhận path → insert Event → refresh list. |
| **P6** | Sau insert event → bắn notification. |

**Milestone Day 7:** **Arm → motion → record → event → notify** chạy được 1 vòng.

---

### Day 8 — Events & Settings sâu

| Người | Việc cần làm |
|-------|----------------|
| **P1** | Màn/chi tiết mở video (VideoView hoặc Intent player). |
| **P2** | (Optional) Torch/flash toggle nếu còn bandwidth. |
| **P3** | Giới hạn: đang ghi thì bỏ qua request mới; toast “Đang ghi…”. |
| **P4** | Map SeekBar Settings → threshold thật (test 3 mức thấp/vừa/cao). |
| **P5** | Màn chi tiết event: thời gian, path, nút Xóa (DB + file). |
| **P6** | Màn PIN lúc mở app (đúng mới vào Main); sai thì báo lỗi. |

---

### Day 9 — Hoàn tính năng

| Người | Việc cần làm |
|-------|----------------|
| **P1** | Polish Monitor: trạng thái Armed, “Recording…”, indicator motion. |
| **P2** | Test xoay màn hình / ra vào app; document limitation nếu không hỗ trợ rotate. |
| **P3** | Thumbnail (frame đầu hoặc icon); hoặc để P5 dùng icon mặc định. |
| **P4** | Giảm false alarm ánh sáng (threshold + cooldown tinh chỉnh). |
| **P5** | Empty state “Chưa có sự kiện”; pull-to-refresh hoặc auto refresh. |
| **P6** | Đổi PIN trong Settings (nhập PIN cũ → PIN mới); validate. |

**Milestone Day 9:** feature freeze nhẹ — chỉ bugfix sau đó.

---

### Day 10 — Ổn định

| Người | Việc cần làm |
|-------|----------------|
| **P1** | Fix crash top priority từ nhóm; chuẩn hóa string/UI. |
| **P2** | Flow deny permission → giải thích + nút mở Settings hệ thống. |
| **P3** | Test hết bộ nhớ / path null → không crash. |
| **P4** | Test phòng tối / quá sáng; ghi note kết quả. |
| **P5** | Xóa nhiều event liên tiếp không lỗi; sort mới nhất trước. |
| **P6** | Notify đúng khi app foreground/background (best-effort). |

---

### Day 11 — UX & edge cases

| Người | Việc cần làm |
|-------|----------------|
| **P1** | Loading/error toast thống nhất; rà soát UX 3 màn. |
| **P2** | Label rõ camera trước/sau đang dùng. |
| **P3** | Hiển thị thời lượng clip (nếu lấy được; không thì bỏ). |
| **P4** | Fine-tune mặc định sensitivity cho demo ổn. |
| **P5** | Format thời gian đẹp (`HH:mm · dd/MM`). |
| **P6** | Copy thông báo VI rõ: “Phát hiện chuyển động — đã lưu clip”. |

**Milestone Day 11:** app ổn để test hệ thống.

---

### Day 12 — Test nhóm

Mỗi người tự test phần mình + chạy full checklist; P1 tổng hợp bug.

| Người | Focus test |
|-------|------------|
| **P1** | Điều phối checklist; gán bug → owner; merge hotfix. |
| **P2** | Permission, preview, đổi camera, lifecycle. |
| **P3** | Record thủ công + auto; file mở được. |
| **P4** | Armed/Disarmed đúng; cooldown; sensitivity. |
| **P5** | List/chi tiết/xóa; data còn sau restart. |
| **P6** | PIN đúng/sai; đổi PIN; notification. |

**Checklist chung:**

- [ ] Cài mới → cấp quyền camera/mic
- [ ] Set PIN → mở app bằng PIN
- [ ] Arm → tạo được ≥ 2 event bằng chuyển động
- [ ] Xem lại clip từ Events
- [ ] Xóa 1 event (DB + file)
- [ ] Disarm → không tạo event mới
- [ ] Đổi độ nhạy → hành vi detect thay đổi rõ

---

### Day 13 — Demo prep

| Người | Việc cần làm |
|-------|----------------|
| **P1** | Script demo 3 phút; quay video backup; tag `v1.0-rc`. |
| **P2** | Chuẩn bị máy demo + trả lời Q&A CameraX. |
| **P3** | Đảm bảo folder clip sạch / vài clip mẫu đẹp. |
| **P4** | Slide 1 trang: cách motion detect hoạt động. |
| **P5** | Screenshot Events; ghi hướng dẫn xem lại clip. |
| **P6** | Slide tổng (ý tưởng, tính năng, kiến trúc, hạn chế, hướng phát triển). |

---

### Day 14 — Nộp & trình bày

| Người | Việc cần làm |
|-------|----------------|
| **P1** | Freeze code sáng; merge cuối; push `main`; dẫn demo. |
| **P2** | Support live demo camera; xử lý sự cố máy. |
| **P3** | Kiểm tra trước giờ demo: ghi được trên máy nộp. |
| **P4** | Demo motion live (đứng đúng góc/cam). |
| **P5** | Backup screenshot nếu cần trong báo cáo. |
| **P6** | Trình bày slide + phần Settings/PIN; nộp báo cáo. |

---

## Quy ước làm việc

1. **Interface chung do P1 định nghĩa sớm (Day 2–3)**  
   Ví dụ: `MotionListener`, `RecordingController`, `EventRepository`, `AlertManager`
2. **Merge vào `main` mỗi ngày cuối buổi** (tối thiểu Day 3, 5, 7, 9, 11)
3. **Máy thật bắt buộc** từ Day 3 (emulator camera kém)
4. **Không sửa module người khác** trừ khi owner OK hoặc P1 assign hotfix
5. **Daily standup 10 phút:** hôm qua / hôm nay / blocker
6. **Blocker > 2 giờ** → báo P1 ngay

### Nhịp mỗi ngày (Day 2–11)

| Mốc | Việc |
|-----|------|
| Đầu buổi (10’) | Standup |
| Cuối buổi | Commit + push branch; P1 merge nếu sẵn sàng |

---

## Nếu trễ tiến độ — cắt theo thứ tự

1. Flash / đổi camera trước  
2. PIN  
3. Xem video trong app (chỉ mở bằng player hệ thống)  
4. **Giữ bằng mọi giá:** Preview + Motion + Record + Event list  

---

## Deliverable cuối kỳ

- [ ] Live camera + Arm/Disarm  
- [ ] Motion → auto record ngắn  
- [ ] Events list + xem/xóa  
- [ ] Notification khi có sự kiện  
- [ ] Settings (độ nhạy, PIN)  
- [ ] README + slide + demo  

---

## Stack gợi ý

- Java + XML layouts (đúng project hiện tại)
- CameraX (Preview + ImageAnalysis + VideoCapture)
- RecyclerView + Material Components
- Room hoặc SQLite nhẹ cho Events
- SharedPreferences cho Settings / PIN
- NotificationChannel cho cảnh báo

### P3 — ghi video

- Video được ghi bằng CameraX `VideoCapture`, không thu âm thanh.
- Thư mục lưu: `getExternalFilesDir("Videos")/` (thư mục riêng của ứng dụng; không cần quyền lưu trữ chung).
- Tên file: `event_yyyyMMdd_HHmmss.mp4`; mỗi file có JSON cùng tên chứa thời điểm bắt đầu/kết thúc, thời lượng và kích thước.
- Khi P2 bind CameraX, gắn use case đã tạo vào controller:

```java
AppController.get().getCameraXRecordingController().ganBoGhi(videoCapture);
```

- P4 gọi `onMotionDetected`; P1 tự dừng sau 8 giây và chỉ thêm event khi video cùng metadata đã lưu thành công.
- Không ghi chồng: yêu cầu mới trong lúc đang ghi bị bỏ qua. Nếu camera chưa sẵn sàng hoặc ghi lỗi, không tạo event rỗng.
- Notebook kiểm tra quy tắc tên file và metadata bằng video mẫu: `colab/kiem_thu_ghi_video.ipynb`. Ghi camera thật vẫn cần chạy app trên điện thoại Android đã cấp quyền camera.

### Chạy thử ghi video trong Android Studio

1. Mở thư mục dự án bằng Android Studio, chờ Gradle Sync hoàn tất.
2. Chọn điện thoại Android hoặc emulator có camera (Device Manager → Edit → Camera: chọn webcam hoặc Virtual Scene), rồi bấm **Run**.
3. Cho phép quyền camera khi ứng dụng hỏi.
4. Ở màn Giám sát, bấm **Ghi thử 5 giây**. Bấm **Dừng ghi** để kết thúc sớm.
5. Ứng dụng hiện đường dẫn file. Mở **View → Tool Windows → Device Explorer** và vào `sdcard/Android/data/vn.edu.usth.camerasystem/files/Videos/`; kiểm tra cặp `.mp4` và `.json`.

Nút ghi thử này dùng chung `CameraXRecordingController` với ghi tự động khi có chuyển động; không cần bật Armed để kiểm tra ghi thủ công.

---

## Build & chạy

1. Clone repo  
2. Mở bằng Android Studio  
3. Sync Gradle  
4. Chạy trên **máy thật** (khuyến nghị)  
5. Cấp quyền Camera (và Microphone nếu ghi tiếng)

```bash
git clone https://github.com/khang2118/CameraSystem.git
```

---

## Flow demo nộp bài (3 phút)

1. Mở app → nhập PIN  
2. Bật **Armed** → thấy live camera  
3. Vẫy tay trước camera → notification + tự ghi vài giây  
4. Vào **Events** → xem lại clip/ảnh  
5. Vào **Settings** → chỉnh độ nhạy / đổi PIN  

---

*CameraSystem — nhóm 6 thành viên — kế hoạch 14 ngày*
