
package vn.edu.usth.camerasystem.storage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Bang luu thong tin su kien camera trong Room Database.
 */
@Entity(tableName = "events")
public class EventEntity {

    @PrimaryKey
    @NonNull
    public String id;

    public long timestampMs;

    @Nullable
    public String videoPath;

    @Nullable
    public String thumbnailPath;

    @Nullable
    public String note;

    public EventEntity(
            @NonNull String id,
            long timestampMs,
            @Nullable String videoPath,
            @Nullable String thumbnailPath,
            @Nullable String note
    ) {
        this.id = id;
        this.timestampMs = timestampMs;
        this.videoPath = videoPath;
        this.thumbnailPath = thumbnailPath;
        this.note = note;
    }

    @NonNull
    public static EventEntity fromSecurityEvent(
            @NonNull SecurityEvent event
    ) {
        return new EventEntity(
                event.getId(),
                event.getTimestampMs(),
                event.getVideoPath(),
                event.getThumbnailPath(),
                event.getNote()
        );
    }

    @NonNull
    public SecurityEvent toSecurityEvent() {
        return new SecurityEvent(
                id,
                timestampMs,
                videoPath,
                thumbnailPath,
                note
        );
    }
}

