package com.example.hospimanagmenetapp.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.hospimanagmenetapp.data.entities.Appointment;

import java.util.List;

@Dao
public interface AppointmentDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(Appointment appt);

    @Update
    int update(Appointment appt);

    @Query("SELECT * FROM appointments WHERE startTime BETWEEN :start AND :end ORDER BY startTime ASC")
    List<Appointment> findBetween(long start, long end);

    @Query("SELECT * FROM appointments WHERE clinicianId = :clinicianId AND "
            + "( (startTime < :newEnd AND endTime > :newStart) )")
    List<Appointment> overlapping(long clinicianId, long newStart, long newEnd);
}
