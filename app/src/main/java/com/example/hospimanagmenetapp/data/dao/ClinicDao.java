package com.example.hospimanagmenetapp.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.hospimanagmenetapp.data.entities.Clinic;
import java.util.List;

@Dao
public interface ClinicDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<Clinic> clinic);

    @Query("SELECT * FROM clinics ORDER BY name ASC")
    List<Clinic> getAll();
}
