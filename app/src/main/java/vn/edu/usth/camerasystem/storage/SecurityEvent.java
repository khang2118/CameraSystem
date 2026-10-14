package vn.edu.usth.camerasystem.storage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Model sự kiện an ninh — dùng chung cho EventRepository (P5) và UI Events.
 */
public class SecurityEvent {

    @NonNull
    private final String id;
    private final long timestampMs;
    @Nullable
    private final String videoPath;
    @Nullable
    private final String thumbnailPath;
    @Nullable
    private final String note;

    public SecurityEvent(@NonNull String id,
                         long timestampMs,
                         @Nullable String videoPath,
                         @Nullable String thumbnailPath,
                         @Nullable String note) {
        this.id = id;
        this.timestampMs = timestampMs;
        this.videoPath = videoPath;
        this.thumbnailPath = thumbnailPath;
        this.note = note;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    @Nullable
    public String getVideoPath() {
        return videoPath;
    }

    @Nullable
    public String getThumbnailPath() {
        return thumbnailPath;
    }

    @Nullable
    public String getNote() {
        return note;
    }
}
