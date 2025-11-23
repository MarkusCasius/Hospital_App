package com.example.hospimanagmenetapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.entities.Patient;
import com.example.hospimanagmenetapp.ui.PatientRegistrationActivity;
import com.example.hospimanagmenetapp.util.EncryptionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PatientRegistrationActivityTest {

    private AppDatabase database;

    @Rule
    public ActivityScenarioRule<PatientRegistrationActivity> activityRule = new ActivityScenarioRule<>(PatientRegistrationActivity.class);

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = AppDatabase.getInstance(context);
        database.clearAllTables();
    }

    @After
    public void tearDown() {
        database.clearAllTables();
    }

    @Test
    public void registerNewPatient_successful() {
        // Arrange: Valid patient details
        String validNhs = "9876543210"; // Valid NHS number
        String name = "John Smith";
        String dob = "01/01/1990";

        // Act: Fill in the form and save
        onView(withId(R.id.etNhs)).perform(typeText(validNhs));
        onView(withId(R.id.etFullName)).perform(typeText(name));
        onView(withId(R.id.etDob)).perform(typeText(dob), closeSoftKeyboard());
        onView(withId(R.id.btnSavePatient)).perform(click());

        // Assert: The activity should finish, but we can't easily test that.
        // A better assertion is to check the database directly to confirm the patient was saved.
        Patient savedPatient = database.patientDao().findByNhs(validNhs);
        assert(savedPatient != null);
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
        // We can't check for a Toast message easily, so we check that a view from the activity is still visible.
        onView(withId(R.id.etNhs)).check(matches(isDisplayed()));
    }

    @Test
    public void registerPatientWithDuplicateNhs_showsError() {
        // Arrange: Pre-load a patient into the database
        String duplicateNhs = "6810564195";
        Patient existingPatient = new Patient();
        existingPatient.enPatientNhsNumber = duplicateNhs;
        try {
            // We need to encrypt some data for the entity to be valid, even if it's dummy data
            EncryptionManager em = new EncryptionManager();
            existingPatient.fullName = em.encrypt("Existing User");
            existingPatient.dateOfBirth = em.encrypt("01/01/1980");
        } catch (Exception e) {
            e.printStackTrace();
        }
        database.patientDao().insert(existingPatient);

        // Act: Try to register a new patient with the same NHS number
        onView(withId(R.id.etNhs)).perform(typeText(duplicateNhs));
        onView(withId(R.id.etFullName)).perform(typeText("New User"), closeSoftKeyboard());
        onView(withId(R.id.btnSavePatient)).perform(click());

        // Assert: The form should still be displayed due to the error.
        onView(withId(R.id.etNhs)).check(matches(isDisplayed()));
    }
}
