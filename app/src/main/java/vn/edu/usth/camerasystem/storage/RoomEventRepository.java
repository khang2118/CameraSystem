
package vn.edu.usth.camerasystem.storage;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RoomEventRepository implements EventRepository {

    private static final String TAG = "RoomEventRepository";

    private final EventDao eventDao;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private final List<SecurityEvent> cache = new ArrayList<>();

    private final Object lock = new Object();

    @Nullable
    private Observer observer;

    public RoomEventRepository(@NonNull Context context) {

        EventDatabase database =
                EventDatabase.getInstance(context);

        eventDao = database.eventDao();

        loadEventsFromDatabase();
    }

    /**
     * Nap du lieu da luu tu Room vao RAM.
     */
    private void loadEventsFromDatabase() {

        executor.execute(() -> {

            try {
                List<EventEntity> entities =
                        eventDao.getAllEvents();

                synchronized (lock) {

                    cache.clear();

                    for (EventEntity entity : entities) {
                        cache.add(entity.toSecurityEvent());
                    }
                }

                notifyObserver();

            } catch (Exception e) {
                Log.e(TAG, "Cannot load events", e);
            }
        });
    }

    /**
     * Them event vao database.
     */
    @Override
    public void addEvent(@NonNull SecurityEvent event) {

        executor.execute(() -> {

            try {
                eventDao.insert(
                        EventEntity.fromSecurityEvent(event)
                );

                refreshCache();

            } catch (Exception e) {
                Log.e(TAG, "Cannot insert event", e);
            }
        });
    }

    /**
     * Lay danh sach event tu cache.
     */
    @NonNull
    @Override
    public List<SecurityEvent> getAllEvents() {

        synchronized (lock) {
            return new ArrayList<>(cache);
        }
    }

    /**
     * Tim event theo ID trong cache.
     */
    @Nullable
    @Override
    public SecurityEvent getEventById(@NonNull String id) {

        synchronized (lock) {

            for (SecurityEvent event : cache) {

                if (event.getId().equals(id)) {
                    return event;
                }
            }
        }

        return null;
    }

    /**
     * Yeu cau xoa event.
     *
     * Luu y: ket qua true nghia la yeu cau duoc chap nhan.
     * Viec xoa database duoc thuc hien bat dong bo.
     */
    @Override
    public boolean deleteEvent(@NonNull String id) {

        if (getEventById(id) == null) {
            return false;
        }

        executor.execute(() -> {

            try {
                int deleted = eventDao.deleteById(id);

                if (deleted > 0) {
                    refreshCache();
                }

            } catch (Exception e) {
                Log.e(TAG, "Cannot delete event", e);
            }
        });

        return true;
    }

    /**
     * Dang ky Observer cho giao dien Events.
     */
    @Override
    public void setObserver(@Nullable Observer observer) {

        synchronized (lock) {
            this.observer = observer;
        }

        if (observer != null) {
            notifyObserver();
        }
    }

    /**
     * Doc lai database sau khi them/xoa.
     */
    private void refreshCache() {

        List<EventEntity> entities =
                eventDao.getAllEvents();

        synchronized (lock) {

            cache.clear();

            for (EventEntity entity : entities) {
                cache.add(entity.toSecurityEvent());
            }
        }

        notifyObserver();
    }

    /**
     * Gui danh sach moi cho UI tren Main Thread.
     */
    private void notifyObserver() {

        mainHandler.post(() -> {

            Observer currentObserver;

            synchronized (lock) {
                currentObserver = observer;
            }

            if (currentObserver != null) {
                currentObserver.onEventsChanged(
                        getAllEvents()
                );
            }
        });
    }
}
