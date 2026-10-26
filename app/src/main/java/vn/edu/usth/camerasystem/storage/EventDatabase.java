
package vn.edu.usth.camerasystem.storage;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Room Database cho module Events + Storage.
 * Luu thong tin su kien camera, khong luu file video truc tiep.
 */
@Database(
        entities = {EventEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class EventDatabase extends RoomDatabase {

    private static volatile EventDatabase INSTANCE;

    public abstract EventDao eventDao();

    @NonNull
    public static EventDatabase getInstance(@NonNull Context context) {

        if (INSTANCE == null) {
            synchronized (EventDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            EventDatabase.class,
                            "camera_events.db"
                    ).build();
                }
            }
        }

        return INSTANCE;
    }
}
