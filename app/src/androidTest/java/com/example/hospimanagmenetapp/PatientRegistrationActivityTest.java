package com.example.hospimanagmenetapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.entities.Patient;
import com.example.hospimanagmenetapp.ui.PatientRegistrationActivity;
import com.example.hospimanagmenetapp.security.EncryptionManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PatientRegistrationActivityTest {

    private AppDatabase database;
    private EncryptionManager encryptionManager;


    @Rule
    public ActivityScenarioRule<PatientRegistrationActivity> activityRule = new ActivityScenarioRule<>(PatientRegistrationActivity.class);

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        database = AppDatabase.getInstance(context);
        encryptionManager = new EncryptionManager();
        database.clearAllTables();
    }

    @After
    public void tearDown() {
        database.clearAllTables();
    }

    @Test
    public void registerNewPatient_successful() throws Exception {
        // Arrange: Valid patient details including all new fields
        String validNhs = "9876543210";
        String name = "John Smith";
        String phone = "07123456789";
        String email = "john.smith@test.com";

        // Act: Fill in all the fields in the form and save
        onView(withId(R.id.etNhs)).perform(typeText(validNhs));
        onView(withId(R.id.etFullName)).perform(typeText(name));
        onView(withId(R.id.etDob)).perform(click());
        onView(ViewMatchers.withText("OK")).perform(click());
        onView(withId(R.id.etPhone)).perform(typeText(phone));
        onView(withId(R.id.etEmail)).perform(typeText(email), closeSoftKeyboard());
        onView(withId(R.id.btnSavePatient)).perform(click());

        // Wait for background thread to finish
        Thread.sleep(2000);

        // Assert: Verify the patient was saved by fetching all patients, decrypting, and finding a match.
        List<Patient> allPatients = database.patientDao().getAll();
        Patient savedPatient = null;
        for (Patient p : allPatients) {
            String decryptedNhs = encryptionManager.decrypt(p.enPatientNhsNumber);
            if (validNhs.equals(decryptedNhs)) {
                savedPatient = p;
                break;
            }
        }

        // Assert that a matching patient was found
        Assert.assertNotNull("A patient with the registered NHS number should be found in the database", savedPatient);

        // Decrypt and verify all other fields were saved correctly
        Assert.assertEquals("Saved name should match input", name, encryptionManager.decrypt(savedPatient.fullName));
        Assert.assertEquals("Saved email should match input", email, encryptionManager.decrypt(savedPatient.email));
        Assert.assertEquals("Saved phone should match input", phone, encryptionManager.decrypt(savedPatient.phone));
    }

    @Test
    public void registerPatientWithInvalidNhs_showsError() {
        // Arrange: Invalid patient details
        String invalidNhs = "1234567890"; // Invalid NHS number

        // Act: Fill in the form and save
        onView(withId(R.id.etNhs)).perform(typeText(invalidNhs));
        onView(withId(R.id.etFullName)).perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.btnSavePatient)).perform(click());

        // Assert: The registration form should still be displayed because of the error.
        onView(withId(R.id.etNhs)).check(matches(isDisplayed()));
    }

    @Test
    public void registerPatientWithDuplicateNhs_showsError() throws Exception {
        // Arrange: Pre-load a patient into the database
        String duplicateNhs = "6810564195";
        Patient existingPatient = new Patient();
        existingPatient.enPatientNhsNumber = encryptionManager.encrypt(duplicateNhs);
        existingPatient.fullName = encryptionManager.encrypt("Existing User");
        existingPatient.dateOfBirth = encryptionManager.encrypt("01/01/1980");
        database.patientDao().insert(existingPatient);

        // Act: Try to register a new patient with the same NHS number
        onView(withId(R.id.etNhs)).perform(typeText(duplicateNhs));
        onView(withId(R.id.etFullName)).perform(typeText("New User"), closeSoftKeyboard());
        onView(withId(R.id.btnSavePatient)).perform(click());

        // Assert: The form should still be displayed due to the error.
        onView(withId(R.id.etNhs)).check(matches(isDisplayed()));
    }

    @Test
    public void registerPatient_withMissingName_fails() {
        // Arrange: Fill all fields EXCEPT the name
        onView(withId(R.id.etNhs)).perform(typeText("9876543210"));
        onView(withId(R.id.etDob)).perform(click());
        onView(ViewMatchers.withText("OK")).perform(click());

        // Act: Click save
        onView(withId(R.id.btnSavePatient)).perform(click());

        // Assert: The activity should still be displayed because registration failed.
        onView(withId(R.id.etFullName)).check(matches(isDisplayed()));
    }

    @Test
    public void registerPatient_withInvalidNhsNumber_fails() {
        // Arrange: Fill fields with an invalid NHS number
        onView(withId(R.id.etNhs)).perform(typeText("1111111112")); // Fails checksum
        onView(withId(R.id.etFullName)).perform(typeText("Test Name"));
        onView(withId(R.id.etDob)).perform(click());
        onView(ViewMatchers.withText("OK")).perform(click());

        // Act: Click save
        onView(withId(R.id.btnSavePatient)).perform(click());

        // Assert: The activity should still be displayed.
        onView(withId(R.id.etNhs)).check(matches(isDisplayed()));
    }

    @Test
    public void registerPatient_withSqlInjectionAttempt_doesNotCrash() {
        // Arrange: Input a common SQL injection pattern into multiple fields
        String sqlInjectionString = "' OR 1=1; --";
        onView(withId(R.id.etNhs)).perform(typeText("1234567890")); // Use a valid-length but incorrect NHS
        onView(withId(R.id.etFullName)).perform(typeText("Hacker Name" + sqlInjectionString));
        onView(withId(R.id.etDob)).perform(click());
        onView(ViewMatchers.withText("OK")).perform(click());
        onView(withId(R.id.etPhone)).perform(typeText("07111222333"));
        onView(withId(R.id.etEmail)).perform(typeText(sqlInjectionString + "@hacker.com"), closeSoftKeyboard());

        // Act: Click save
        onView(withId(R.id.btnSavePatient)).perform(click());

        // Assert: The app should not crash. The validation for the NHS number will likely fail first.
        // We verify the activity is still open as a proxy for not crashing.
        onView(withId(R.id.etNhs)).check(matches(isDisplayed()));
    }
}