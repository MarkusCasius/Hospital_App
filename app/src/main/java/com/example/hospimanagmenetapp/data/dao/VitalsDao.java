package com.example.hospimanagmenetapp.data.dao;

import androidx.room.*;
import com.example.hospimanagmenetapp.data.entities.Vitals;
import java.util.List;

@Dao
public interface VitalsDao {
    @Query("SELECT * FROM vitals WHERE synced=0")
    List<Vitals> getPending();

    @Insert
    long insert(Vitals v);

    @Query("UPDATE vitals SET synced=1 WHERE id=:id")
    void markSynced(long id);
}

