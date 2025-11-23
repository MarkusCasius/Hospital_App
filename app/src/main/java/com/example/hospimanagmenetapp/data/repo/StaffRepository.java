package com.example.hospimanagmenetapp.data.repo;

import android.content.Context;
import android.util.Log;import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.dao.StaffDao;
import com.example.hospimanagmenetapp.data.entities.Staff;
import com.example.hospimanagmenetapp.network.ApiClient;
import com.example.hospimanagmenetapp.network.dto.StaffDto; // Assuming this DTO exists

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

public class StaffRepository {

    private static final String TAG = "StaffRepository";
    private final StaffDao staffDao;
    private final ApiClient api;

    public StaffRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.staffDao = db.staffDao();
        this.api = new ApiClient(context);
    }

    private void refreshStaffCache() {
        try {
            Log.d(TAG, "Fetching staff from network to refresh cache.");
            Response<List<StaffDto>> response = api.staffApi().getAllStaff().execute();
            if (response.isSuccessful() && response.body() != null) {
                List<Staff> staffToCache = new ArrayList<>();
                for (StaffDto dto : response.body()) {
                    // Map DTO to Entity
                    Staff staff = new Staff();
                    staff.id = dto.id;
                    staff.fullName = dto.fullName;
                    staff.email = dto.email;
                    staff.adminPin = dto.adminPin;
                    try {
                        staff.role = Staff.Role.valueOf(dto.role);
                        if (dto.expertise != null) {
                            staff.expertise = Staff.Expertise.valueOf(dto.expertise);
                        }
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Unknown role or expertise from API: " + dto.role + ", " + dto.expertise);
                        continue; // Skip this invalid record
                    }
                    staffToCache.add(staff);
                }

                for(Staff staff : staffToCache) {
                    staffDao.insert(staff);
                }
                Log.d(TAG, "Successfully cached " + staffToCache.size() + " staff from network.");
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to sync staff from network. Will use local data.", e);
        }
    }

    public List<Staff> getAndCacheAllStaff() {
        refreshStaffCache();
        return staffDao.getAll();
    }

    public List<Staff> getAndCacheClinicians() {
        refreshStaffCache();
        Log.d(TAG, "Fetching clinicians from local database.");
        return staffDao.getClinicians();
    }

    public void registerStaff(Staff staff) throws Exception {
        StaffDto dto = mapToDto(staff); // Map entity to DTO

        Log.d(TAG, "Registering staff via network: " + staff.email);
        Response<StaffDto> response = api.staffApi().registerStaff(dto).execute();

        // 2. On success, save the response to the local DAO
        if (response.isSuccessful() && response.body() != null) {
            Log.d(TAG, "Network registration successful. Caching to local DB.");
            Staff savedStaff = mapToEntity(response.body()); // Map response DTO back to entity
            staffDao.insert(savedStaff);
        } else {
            throw new IOException("API Error: " + response.code() + " " + response.message());
        }
    }

    public void deleteStaff(Staff staff) throws Exception{
        Log.d(TAG, "Deleting staff via network: ID " + staff.id);
        Response<Void> response = api.staffApi().deleteStaff(staff.id).execute();

        if (response.isSuccessful()) {
            Log.d(TAG, "Network deletion successful. Deleting from local DB.");
            staffDao.delete(staff);
        } else {
            throw new IOException("API Error: " + response.code() + " " + response.message());
        }
    }

    // --- Help Mappers ---

    private StaffDto mapToDto(Staff staff) {
        StaffDto dto = new StaffDto();
        dto.id = staff.id;
        dto.fullName = staff.fullName;
        dto.email = staff.email;
        dto.role = staff.role.name();
        dto.adminPin = staff.adminPin;
        if (staff.expertise != null) {
            dto.expertise = staff.expertise.name();
        }
        return dto;
    }

    private Staff mapToEntity(StaffDto dto) {
        Staff staff = new Staff();
        staff.id = dto.id;
        staff.fullName = dto.fullName;
        staff.email = dto.email;
        staff.adminPin = dto.adminPin;
        try {
            staff.role = Staff.Role.valueOf(dto.role);
            if (dto.expertise != null) {
                staff.expertise = Staff.Expertise.valueOf(dto.expertise);
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Unknown role or expertise from API: " + dto.role);
        }
        return staff;
    }
}
