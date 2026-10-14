package vn.edu.usth.camerasystem.storage.stub;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import vn.edu.usth.camerasystem.storage.EventRepository;
import vn.edu.usth.camerasystem.storage.SecurityEvent;

/**
 * Stub Day 3 — giữ event trong RAM. P5 thay bằng Room/SQLite.
 */
public class InMemoryEventRepository implements EventRepository {

    private final Map<String, SecurityEvent> events = new LinkedHashMap<>();
    @Nullable
    private Observer observer;

    @Override
    public synchronized void addEvent(@NonNull SecurityEvent event) {
        events.put(event.getId(), event);
        notifyObserver();
    }

    @NonNull
    @Override
    public synchronized List<SecurityEvent> getAllEvents() {
        List<SecurityEvent> list = new ArrayList<>(events.values());
        Collections.reverse(list); // mới nhất trước
        return list;
    }

    @Nullable
    @Override
    public synchronized SecurityEvent getEventById(@NonNull String id) {
        return events.get(id);
    }

    @Override
    public synchronized boolean deleteEvent(@NonNull String id) {
        boolean removed = events.remove(id) != null;
        if (removed) {
            notifyObserver();
        }
        return removed;
    }

    @Override
    public synchronized void setObserver(@Nullable Observer observer) {
        this.observer = observer;
        if (observer != null) {
            observer.onEventsChanged(getAllEvents());
        }
    }

    private void notifyObserver() {
        if (observer != null) {
            observer.onEventsChanged(getAllEvents());
        }
    }
}
