Vai trò: P1 Lead/UI · P2 Camera · P3 Recording · P4 Motion · P5 Events/Storage · P6 Settings/Alert/PIN

Day 1 — Kickoff & setup
Người	Việc cần làm (DONE khi…)
P1
Tạo package ui/camera/motion/storage/alert; bottom nav 3 tab trống; README ngắn quy ước branch/merge. Done: app mở được 3 màn trống.
P2
Nghiên cứu CameraX Preview; liệt kê dependency cần thêm vào libs.versions.toml. Done: PR/list dependency + note máy test.
P3
Nghiên cứu CameraX VideoCapture; quyết định nơi lưu file (getExternalFilesDir). Done: note path + API dự kiến.
P4
Prototype so sánh 2 ảnh trên giấy/code nhỏ (luminance). Done: hàm demo nhận 2 Bitmap → true/false.
P5
Thiết kế model Event (id, timestamp, videoPath, thumbnailPath, note). Done: schema chốt với nhóm.
P6
Wireframe Settings + list key SharedPreferences (armed, sensitivity, pin). Done: mock UI XML hoặc sketch.


Day 2 — Nền tảng
Người	Việc cần làm
P1
Tạo MainActivity + 3 Fragment (Monitor, Events, Settings); theme/colors cơ bản.
P2
Thêm CameraX deps; xin quyền CAMERA trong Manifest + runtime.
P3
Xin quyền RECORD_AUDIO (nếu ghi tiếng); tạo folder lưu video.
P4
Viết class MotionDetector (input: byte/Bitmap, output: score + isMotion).
P5
Setup Room/SQLite: EventDao, EventDatabase.
P6
Tạo SettingsFragment UI: SeekBar độ nhạy, Switch Armed, ô PIN.


Day 3 — Module chạy riêng
Người	Việc cần làm
P1
Định nghĩa interface chung: RecordingController, MotionListener, EventRepository, AlertManager.
P2
Preview hiển thị trên MonitorFragment (máy thật).
P3
Nút Record thủ công: start 5s → stop → log path file.
P4
Unit test/manual: thay đổi threshold → kết quả motion đổi.
P5
Insert 3 event giả → hiển thị RecyclerView.
P6
Đọc/ghi SharedPreferences; Armed switch lưu được qua restart app.
Milestone Day 3: mỗi người demo được phần của mình trên branch.


Day 4 — Mở rộng module
Người	Việc cần làm
P1
ViewModel/AppController điều phối Arm state; merge sớm code P2 vào main nếu ổn.
P2
Xử lý lifecycle: pause/stop camera khi rời màn; không leak.
P3
API startRecording() / stopRecording(callback path); chống double-start.
P4
Gắn ImageAnalysis (phối hợp P2) — nhận frame liên tục.
P5
Adapter Events: hiện giờ + icon; click item (chưa cần play).
P6
NotificationHelper: tạo channel, hàm showMotionAlert(title, body).


Day 5 — Sẵn sàng ghép
Người	Việc cần làm
P1
Checklist merge Day 6; fix conflict navigation/permissions.
P2
Nút đổi camera trước/sau trên Monitor.
P3
Đặt tên file event_yyyyMMdd_HHmmss.mp4; test ghi 3 lần liên tiếp.
P4
Thêm cooldown 10s sau mỗi lần detect; log rõ.
P5
Hàm addEvent(path, time) dùng được từ module khác.
P6
Nút Armed/Disarmed trên Monitor (bind Prefs); Disarmed → không alert.
Milestone Day 5: interface + module độc lập xong.


Day 6 — Ghép luồng A (Camera + Motion + Arm)
Người	Việc cần làm
P1
Merge P2+P4; điều phối session ghép; ghi bug list.
P2
Preview + ImageAnalysis chạy song song ổn định.
P3
Standby: chuẩn bị hook nhận tín hiệu từ P4.
P4
Khi Armed + motion → gọi onMotionDetected().
P5
Quan sát API; chuẩn bị insert khi có path thật.
P6
Chỉ cho phép detect/alert khi armed == true.
Done Day 6: vẫy tay trước cam → log/motion callback chạy (chưa bắt buộc đã ghi video).


Day 7 — Ghép luồng B (Record + Event + Notify)
Người	Việc cần làm
P1
Nối full pipeline trên main; demo nhóm cuối ngày.
P2
Fix crash khi vừa analyze vừa record (nếu có).
P3
onMotionDetected → auto record 5–10s → trả path.
P4
Trong lúc đang record: tạm bỏ qua motion mới (tránh spam).
P5
Nhận path → insert Event → refresh list.
P6
Sau insert event → bắn notification.
Milestone Day 7: Arm → motion → record → event → notify chạy được 1 vòng.


Day 8 — Events & Settings sâu
Người	Việc cần làm
P1
Màn/chi tiết mở video (VideoView hoặc Intent player).
P2
(Optional) Torch/flash toggle nếu còn bandwidth.
P3
Giới hạn: đang ghi thì bỏ qua request mới; toast “Đang ghi…”.
P4
Map SeekBar Settings → threshold thật (test 3 mức thấp/vừa/cao).
P5
Màn chi tiết event: thời gian, path, nút Xóa (DB + file).
P6
Màn PIN lúc mở app (đúng mới vào Main); sai thì báo lỗi.


Day 9 — Hoàn tính năng
Người	Việc cần làm
P1
Polish Monitor: trạng thái Armed, “Recording…”, indicator motion.
P2
Test xoay màn hình / ra vào app; document limitation nếu không hỗ trợ rotate.
P3
Thumbnail (frame đầu hoặc icon); hoặc để P5 dùng icon mặc định.
P4
Giảm false alarm ánh sáng (threshold + cooldown tinh chỉnh).
P5
Empty state “Chưa có sự kiện”; pull-to-refresh hoặc auto refresh.
P6
Đổi PIN trong Settings (nhập PIN cũ → PIN mới); validate.
Milestone Day 9: feature freeze nhẹ — chỉ bugfix sau đó.


Day 10 — Ổn định
Người	Việc cần làm
P1
Fix crash top priority từ nhóm; chuẩn hóa string/UI.
P2
Flow deny permission → giải thích + nút mở Settings hệ thống.
P3
Test hết bộ nhớ / path null → không crash.
P4
Test phòng tối / quá sáng; ghi note kết quả.
P5
Xóa nhiều event liên tiếp không lỗi; sort mới nhất trước.
P6
Notify đúng khi app foreground/background (best-effort).


Day 11 — UX & edge cases
Người	Việc cần làm
P1
Loading/error toast thống nhất; rà soát UX 3 màn.
P2
Label rõ camera trước/sau đang dùng.
P3
Hiển thị thời lượng clip (nếu lấy được; không thì bỏ).
P4
Fine-tune mặc định sensitivity cho demo ổn.
P5
Format thời gian đẹp (HH:mm · dd/MM).
P6
Copy thông báo VI rõ: “Phát hiện chuyển động — đã lưu clip”.
Milestone Day 11: app ổn để test hệ thống.


Day 12 — Test nhóm (cùng checklist)
Mỗi người tự test phần mình + chạy full checklist; P1 tổng hợp bug.

Người	Focus test
P1
Điều phối 15 case; gán bug → owner; merge hotfix.
P2
Permission, preview, đổi camera, lifecycle.
P3
Record thủ công + auto; file mở được.
P4
Armed/Disarmed đúng; cooldown; sensitivity.
P5
List/chi tiết/xóa; data còn sau restart.
P6
PIN đúng/sai; đổi PIN; notification.
Checklist chung (cả nhóm): cài mới → cấp quyền → set PIN → Arm → tạo 2 event → xem → xóa 1 → Disarm không tạo event → đổi độ nhạy.


Day 13 — Demo prep
Người	Việc cần làm
P1
Script demo 3 phút; quay video backup; tag v1.0-rc.
P2
Chuẩn bị máy demo + trả lời Q&A CameraX.
P3
Đảm bảo folder clip sạch / vài clip mẫu đẹp.
P4
Slide 1 trang: cách motion detect hoạt động.
P5
Screenshot Events; ghi hướng dẫn xem lại clip trong README.
P6
Slide tổng (ý tưởng, tính năng, kiến trúc, hạn chế, hướng phát triển).


Day 14 — Nộp & trình bày
Người	Việc cần làm
P1
Freeze code sáng; merge cuối; push main; dẫn demo.
P2
Support live demo camera; xử lý sự cố máy.
P3
Kiểm tra trước giờ demo: ghi được trên máy nộp.
P4
Demo motion live (đứng đúng góc/cam).
P5
Backup export DB/screenshot nếu cần trong báo cáo.
P6
Trình bày slide + phần Settings/PIN; nộp README/báo cáo.
