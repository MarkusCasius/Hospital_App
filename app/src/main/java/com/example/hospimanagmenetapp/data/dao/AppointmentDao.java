package com.example.hospimanagmenetapp.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.hospimanagmenetapp.data.entities.Appointment;

import java.util.List;
import androidx.room.Transaction;
@Dao
public interface AppointmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(Appointment appt);

    @Update
    int update(Appointment appt);

    @Query("SELECT * FROM appointments WHERE startTime BETWEEN :start AND :end ORDER BY startTime ASC")
    List<Appointment> findBetween(long start, long end);

    @Query("SELECT * FROM appointments WHERE clinicianId = :clinicianId AND "
            + "( (startTime < :newEnd AND endTime > :newStart) )")
    List<Appointment> overlapping(long clinicianId, long newStart, long newEnd);

    @Query("SELECT * FROM appointments WHERE clinic = :clinic AND startTime >= :start AND startTime < :end")
    List<Appointment> getAppointmentsByClinicBetween(String clinic, long start, long end);

    @Query("SELECT * FROM appointments WHERE startTime >= :start AND startTime < :end")
    List<Appointment> getAllAppointmentsBetween(long start, long end);

}
