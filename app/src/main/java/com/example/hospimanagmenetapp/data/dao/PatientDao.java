package com.example.hospimanagmenetapp.data.dao; // DAO (Data Access Object) package

import androidx.room.Dao;                 // Marks this interface as a Room DAO
import androidx.room.Insert;             // Annotation for insert operations
import androidx.room.OnConflictStrategy; // How Room should behave on key/constraint conflicts
import androidx.room.Query;              // Annotation for custom SQL queries

import com.example.hospimanagmenetapp.data.entities.Patient; // Entity this DAO operates on

@Dao // Tells Room to generate the implementation at compile time
public interface PatientDao {

    @Insert(onConflict = OnConflictStrategy.ABORT) // Insert a Patient; fail if a constraint (e.g., unique NHS) is violated
    long insert(Patient patient);   // Returns the new row ID (or -1 if ignored, depending on strategy)

    @Query("SELECT COUNT(*) FROM patients WHERE nhsNumber = :nhsNumber") // Parameterised SQL; :nhsNumber is bound from the method arg
    int countByNhs(String nhsNumber);   // Quick existence check (0 = none, >0 = exists)

    @Query("SELECT * FROM patients WHERE nhsNumber = :nhsNumber")
    Patient findByNhs(String nhsNumber);
}