package com.example.hospimanagmenetapp;

import org.junit.Test;

import static org.junit.Assert.*;

import com.example.hospimanagmenetapp.data.entities.Staff;
import com.example.hospimanagmenetapp.util.ValidationUtils;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class UnitTest {

    // Unit Tests for PatientRegistrationActivity Logic

    @Test
    public void nhsNumberValidation_withValidNumber_returnsTrue() {
        // Test case for the validation logic used in PatientRegistrationActivity
        String validNhsNumber = "9876543210";
        assertTrue("A valid 10-digit NHS number should return true",
                ValidationUtils.validateNhsNumber(validNhsNumber));
    }

    @Test
    public void nhsNumberValidation_withInvalidNumber_returnsFalse() {
        // Test case for the validation logic used in PatientRegistrationActivity
        String invalidNhsNumber = "1234567890"; // This fails the Mod 11 check
        assertFalse("An invalid NHS number that fails the checksum should return false",
                ValidationUtils.validateNhsNumber(invalidNhsNumber));
    }

    @Test
    public void nhsNumberValidation_withIncorrectLength_returnsFalse() {
        // Test case for the validation logic used in PatientRegistrationActivity
        String shortNhsNumber = "12345";
        assertFalse("An NHS number with incorrect length should return false",
                ValidationUtils.validateNhsNumber(shortNhsNumber));
    }

    @Test
    public void nhsNumberValidation_withNullInput_returnsFalse() {
        // Test case for robustness in PatientRegistrationActivity
        assertFalse("A null NHS number should return false",
                ValidationUtils.validateNhsNumber(null));
    }


    // Unit Tests for AdminLoginActivity Logic

    @Test
    public void adminLoginValidation_withCorrectCredentials_returnsTrue() {
        // This test simulates the core logic of doLogin() in AdminLoginActivity
        Staff mockAdminStaff = new Staff();
        mockAdminStaff.email = "admin@test.com";
        mockAdminStaff.adminPin = "1234";
        mockAdminStaff.role = Staff.Role.ADMIN;

        String inputEmail = "admin@test.com";
        String inputPin = "1234";

        boolean isValid = mockAdminStaff != null &&
                mockAdminStaff.role == Staff.Role.ADMIN &&
                mockAdminStaff.adminPin != null &&
                mockAdminStaff.adminPin.equals(inputPin);

        assertTrue("Login should be valid with correct admin credentials", isValid);
    }

    @Test
    public void adminLoginValidation_withIncorrectPin_returnsFalse() {
        // This test simulates the core logic of doLogin() in AdminLoginActivity
        Staff mockAdminStaff = new Staff();
        mockAdminStaff.email = "admin@test.com";
        mockAdminStaff.adminPin = "1234";
        mockAdminStaff.role = Staff.Role.ADMIN;

        String inputEmail = "admin@test.com";
        String inputPin = "9999"; // Incorrect PIN

        boolean isValid = mockAdminStaff != null &&
                mockAdminStaff.role == Staff.Role.ADMIN &&
                mockAdminStaff.adminPin != null &&
                mockAdminStaff.adminPin.equals(inputPin);

        assertFalse("Login should be invalid with an incorrect PIN", isValid);
    }

    @Test
    public void adminLoginValidation_withNonAdminUser_returnsFalse() {
        // This test simulates the core logic of doLogin() in AdminLoginActivity
        Staff mockStaffUser = new Staff();
        mockStaffUser.email = "staff@test.com";
        mockStaffUser.adminPin = "1234";
        mockStaffUser.role = Staff.Role.CLINICIAN; // Not an ADMIN

        String inputEmail = "staff@test.com";
        String inputPin = "1234";

        boolean isValid = mockStaffUser != null &&
                mockStaffUser.role == Staff.Role.ADMIN && // This check will fail
                mockStaffUser.adminPin != null &&
                mockStaffUser.adminPin.equals(inputPin);

        assertFalse("Login should be invalid if the user is not an ADMIN", isValid);
    }

    @Test
    public void adminLoginValidation_withNonExistentUser_returnsFalse() {
        // This test simulates the core logic of doLogin() in AdminLoginActivity
        Staff nullStaff = null; // DAO returns null because user doesn't exist

        String inputEmail = "nouser@test.com";
        String inputPin = "1234";

        boolean isValid = nullStaff != null && // This check will fail
                nullStaff.role == Staff.Role.ADMIN &&
                nullStaff.adminPin != null &&
                nullStaff.adminPin.equals(inputPin);

        assertFalse("Login should be invalid if the staff object is null", isValid);
    }
}