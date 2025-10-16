package com.example.hospimanagmenetapp.data.dao; // DAO (Data Access Object) package for Room

import androidx.room.Dao;                 // Marks this interface as a Room DAO
import androidx.room.Insert;             // Annotation for insert operations
import androidx.room.OnConflictStrategy; // Policy for handling conflicts (e.g., unique constraints)
import androidx.room.Query;              // Annotation for custom SQL queries

import com.example.hospimanagmenetapp.data.entities.Staff; // Entity this DAO operates on

import java.util.List; // Used for returning multiple results

@Dao // Instructs Room to generate the implementation at compile time
public interface StaffDao {

    @Insert(onConflict = OnConflictStrategy.ABORT) // Insert a Staff record; fail if a constraint is violated
    long insert(Staff staff);        // Returns the new row ID (throws on conflict with ABORT)

    @Query("SELECT * FROM staff ORDER BY fullName ASC") // Fetch all staff ordered alphabetically by full name
    List<Staff> getAll();    // Returns a snapshot list (call from background thread)

    @Query("SELECT COUNT(*) FROM staff WHERE role = 'ADMIN'") // Count staff whose role is ADMIN
    int countAdmins();     // Useful for gating admin features/bootstrapping

    @Query("SELECT * FROM staff WHERE email = :email LIMIT 1") // Look up a single staff member by email
    Staff findByEmail(String email);       // Returns null if not found (handle in caller)
}