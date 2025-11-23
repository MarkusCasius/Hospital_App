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

    @Query("SELECT * FROM appointments WHERE (:clinic IS NULL OR clinic = :clinic) ORDER BY startTime ASC")
    List<Appointment> getAllAppointmentsForClinic(String clinic);

    @Query("SELECT * FROM appointments WHERE (:clinic IS NULL OR clinic = :clinic) AND startTime >= :startTime AND startTime < :endTime ORDER BY startTime ASC")
    List<Appointment> getAppointmentsForClinicBetween(String clinic, long startTime, long endTime);

    @Query("SELECT * FROM appointments WHERE (:clinic IS NULL OR clinic = :clinic) AND startTime < :endTime ORDER BY startTime DESC")
    List<Appointment> getAppointmentsForClinicBefore(String clinic, long endTime);

    @Query("SELECT * FROM appointments WHERE (:clinic IS NULL OR clinic = :clinic) AND startTime >= :startTime ORDER BY startTime ASC")
    List<Appointment> getAppointmentsForClinicAfter(String clinic, long startTime);

    @Query("SELECT * FROM appointments WHERE clinicianId = :clinicianId " +
            "AND id != :appointmentIdToIgnore " +
            "AND status = 'BOOKED' " +
            "AND ((startTime < :newEndTime AND endTime > :newStartTime))")
    List<Appointment> getConflictingAppointments(long clinicianId, long newStartTime, long newEndTime, long appointmentIdToIgnore);


    @Query("SELECT * FROM appointments WHERE clinicianId = :clinicianId AND "
            + "( (startTime < :newEnd AND endTime > :newStart) )")
    List<Appointment> overlapping(long clinicianId, long newStart, long newEnd);

    @Query("SELECT * FROM appointments WHERE enPatientNhsNumber = :nhsNumber ORDER BY startTime DESC")
    List<Appointment> getAppointmentsForPatient(String nhsNumber);

    @Query("SELECT * FROM appointments ORDER BY startTime DESC")
    List<Appointment> getAllAppointments();

    @Query("SELECT * FROM appointments WHERE clinic = :clinic ORDER BY startTime DESC")
    List<Appointment> getAppointmentsByClinic(String clinic);
}
