package com.example.hospimanagmenetapp.data.dao;

import androidx.paging.PagingSource;
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

    @Query("SELECT COUNT(id) FROM vitals WHERE enPatientNhsNumber = :nhsNumber")
    int getVitalsCountForPatient(String nhsNumber);

    @Query("SELECT * FROM vitals WHERE enPatientNhsNumber = :nhsNumber ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    List<Vitals> getVitalsForPatientPaged(String nhsNumber, int limit, int offset);

    @Query("SELECT * FROM vitals ORDER BY timestamp DESC")
    List<Vitals> getAllVitals();
}
