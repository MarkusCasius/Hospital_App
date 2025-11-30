package com.example.hospimanagmenetapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.entities.Patient;
import com.example.hospimanagmenetapp.data.entities.Staff;
import com.example.hospimanagmenetapp.feature.ehr.ui.PatientSummaryActivity;
import com.example.hospimanagmenetapp.security.EncryptionManager;
import com.example.hospimanagmenetapp.util.DatabaseSeeder;
import com.example.hospimanagmenetapp.util.SessionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PatientSummaryActivityTest {

    private AppDatabase database;
    private EncryptionManager encryptionManager;
    private static final String TEST_PATIENT_NHS = "1234567890";

    @Rule
    public ActivityScenarioRule<PatientSummaryActivity> activityRule = new ActivityScenarioRule<>(
            new Intent(ApplicationProvider.getApplicationContext(), PatientSummaryActivity.class)
                    .putExtra("nhsNumber", "1234567890") // Patient Name 4
                    .putExtra("bypassRbacCheck", true)
    );

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        database = AppDatabase.getInstance(context);
        encryptionManager = new EncryptionManager();

        database.clearAllTables();

        Patient testPatient = new Patient();
        testPatient.enPatientNhsNumber = encryptionManager.encrypt(TEST_PATIENT_NHS);
        testPatient.fullName = encryptionManager.encrypt("Patient Name 4");
        testPatient.dateOfBirth = encryptionManager.encrypt("1990-01-05");
        // Add other fields if necessary for the test
        database.patientDao().insert(testPatient);

        SessionManager.setCurrentUser(context, Staff.Role.ADMIN.name(), "Dummy@email.com");
    }

    @After
    public void tearDown() {
        // Clean up the database after the test
        database.clearAllTables();
    }
    @Test
    public void createAndUpdateClinicalRecord_isSuccessful() {
        // Arrange: The activity is launched with a patient who has no clinical record.
        // The "No clinical record found" toast should appear, and fields should be empty.
        onView(withId(R.id.etProblems)).check(matches(withText("")));

        // Act: Enter new details into the fields
        String newProblems = "Newly discovered allergy";
        onView(withId(R.id.etProblems)).perform(replaceText(newProblems));
        onView(withId(R.id.etAllergies)).perform(replaceText("Peanuts"));
        onView(withId(R.id.btnSave)).perform(click());

        // Assert: After saving, the fields should still contain the new text.
        try { Thread.sleep(2000); } catch (InterruptedException e) {} // Wait for save/reload
        onView(withId(R.id.etProblems)).check(matches(withText(newProblems)));

        // Act: Update the record again
        String updatedProblems = "Allergy confirmed by test";
        onView(withId(R.id.etProblems)).perform(replaceText(updatedProblems));
        onView(withId(R.id.btnSave)).perform(click());

        // Assert: After the second save, the text should be the updated version.
        try { Thread.sleep(2000); } catch (InterruptedException e) {} // Wait for save/reload
        onView(withId(R.id.etProblems)).check(matches(withText(updatedProblems)));
    }
}
