package vn.edu.usth.camerasystem.storage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Lưu / đọc sự kiện (P5 implement bằng Room hoặc SQLite).
 */
public interface EventRepository {

    void addEvent(@NonNull SecurityEvent event);

    @NonNull
    List<SecurityEvent> getAllEvents();

    @Nullable
    SecurityEvent getEventById(@NonNull String id);

    /**
     * Xóa event trong DB (và ideally xóa file video nếu có).
     *
     * @return true nếu xóa được
     */
    boolean deleteEvent(@NonNull String id);

    /** Đăng ký lắng nghe khi danh sách đổi (UI refresh). */
    void setObserver(@Nullable Observer observer);

    interface Observer {
        void onEventsChanged(@NonNull List<SecurityEvent> events);
    }
}
