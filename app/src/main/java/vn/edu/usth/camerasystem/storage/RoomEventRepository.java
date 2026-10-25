package vn.edu.usth.camerasystem.storage;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Room implementation of the team's existing synchronous EventRepository API. */
public final class RoomEventRepository implements EventRepository {
    private static final String TAG = "RoomEventRepository";

    private final Context applicationContext;
    private final EventDao eventDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Object lock = new Object();
    private final List<SecurityEvent> cache = new ArrayList<>();

    @Nullable private Observer observer;

    public RoomEventRepository(@NonNull Context context) {
        applicationContext = context.getApplicationContext();
        eventDao = EventDatabase.getInstance(applicationContext).eventDao();
        io.execute(this::refreshCache);
    }

    @Override
    public void addEvent(@NonNull SecurityEvent event) {
        io.execute(() -> {
            try {
                eventDao.insert(EventEntity.fromSecurityEvent(event));
                refreshCache();
            } catch (Exception ex) {
                Log.e(TAG, "Failed to insert event", ex);
            }
        });
    }

    @NonNull
    @Override
    public List<SecurityEvent> getAllEvents() {
        synchronized (lock) {
            return new ArrayList<>(cache);
        }
    }

    @Nullable
    @Override
    public SecurityEvent getEventById(@NonNull String id) {
        synchronized (lock) {
            for (SecurityEvent event : cache) {
                if (id.equals(event.getId())) return event;
            }
        }
        return null;
    }

    /**
     * Returns true if a known event has been queued for deletion, not if the
     * asynchronous database deletion has finished. The observer reports success.
     */
    @Override
    public boolean deleteEvent(@NonNull String id) {
        if (getEventById(id) == null) return false;
        io.execute(() -> {
            try {
                EventEntity entity = eventDao.getEventById(id);
                if (entity == null) {
                    refreshCache();
                    return;
                }
                int removed = eventDao.deleteById(id);
                if (removed > 0) {
                    SecurityEvent event = entity.toSecurityEvent();
                    deleteIfAppOwned(event.getVideoPath());
                    deleteIfAppOwned(event.getThumbnailPath());
                }
                refreshCache();
            } catch (Exception ex) {
                Log.e(TAG, "Failed to delete event", ex);
            }
        });
        return true;
    }

    @Override
    public void setObserver(@Nullable Observer next) {
        synchronized (lock) {
            observer = next;
        }
        if (next != null) notifyObserver();
    }

    private void refreshCache() {
        try {
            List<EventEntity> entities = eventDao.getAllEvents();
            List<SecurityEvent> snapshot = new ArrayList<>();
            for (EventEntity item : entities) snapshot.add(item.toSecurityEvent());
            synchronized (lock) {
                cache.clear();
                cache.addAll(snapshot);
            }
            notifyObserver();
        } catch (Exception ex) {
            Log.e(TAG, "Failed to load events", ex);
        }
    }

    private void notifyObserver() {
        main.post(() -> {
            Observer target;
            synchronized (lock) {
                target = observer;
            }
            if (target != null) target.onEventsChanged(getAllEvents());
        });
    }

    /** Only delete files in our private storage. Never delete arbitrary paths from DB. */
    private void deleteIfAppOwned(@Nullable String path) {
        if (path == null || path.isEmpty() || path.startsWith("content://")) return;
        try {
            File target = new File(path).getCanonicalFile();
            if (!target.isFile()) return;
            File[] roots = new File[] {
                    applicationContext.getFilesDir(),
                    applicationContext.getCacheDir(),
                    applicationContext.getExternalFilesDir(null),
                    applicationContext.getExternalCacheDir()
            };
            for (File root : roots) {
                if (root == null) continue;
                String rootPath = root.getCanonicalPath() + File.separator;
                if (target.getPath().startsWith(rootPath)) {
                    if (!target.delete()) Log.w(TAG, "Cannot delete media file: " + target);
                    return;
                }
            }
            Log.w(TAG, "Media outside app storage; preserved: " + target);
        } catch (IOException ex) {
            Log.e(TAG, "Cannot resolve media path", ex);
        }
    }
}
