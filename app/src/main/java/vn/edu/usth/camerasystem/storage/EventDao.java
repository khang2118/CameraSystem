package vn.edu.usth.camerasystem.storage;

import androidx.annotation.NonNull;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object (DAO).
 * Thuc hien cac thao tac voi bang events trong Room Database.
 */
@Dao
public interface EventDao {

    // Them mot su kien moi vao database.
    // Neu trung ID thi cap nhat du lieu.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(@NonNull EventEntity event);

    // Lay danh sach su kien, moi nhat hien thi truoc.
    @NonNull
    @Query("SELECT * FROM events ORDER BY timestampMs DESC")
    List<EventEntity> getAllEvents();

    // Tim mot su kien theo ID.
    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    EventEntity getEventById(@NonNull String id);

    // Xoa mot su kien theo ID.
    // Tra ve so dong da xoa.
    @Query("DELETE FROM events WHERE id = :id")
    int deleteById(@NonNull String id);

    // Dem tong so su kien.
    @Query("SELECT COUNT(*) FROM events")
    int countEvents();
}

