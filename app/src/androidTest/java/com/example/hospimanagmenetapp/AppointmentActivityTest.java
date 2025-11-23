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
import android.content.Intent;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.entities.Staff;
import com.example.hospimanagmenetapp.ui.AppointmentActivity;
import com.example.hospimanagmenetapp.util.DatabaseSeeder;
import com.example.hospimanagmenetapp.util.SessionManager;

import org.hamcrest.Matcher;
import static org.hamcrest.Matchers.equalTo;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;



@RunWith(AndroidJUnit4.class)
@LargeTest
public class AppointmentActivityTest {

    private AppDatabase database;

    @Rule
    public ActivityScenarioRule<AppointmentActivity> activityRule = new ActivityScenarioRule<>(
            new Intent(ApplicationProvider.getApplicationContext(), AppointmentActivity.class)
                    .putExtra("bypassRbacCheck", true)
    );

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = AppDatabase.getInstance(context);
        database.clearAllTables();

        DatabaseSeeder.seed(context);
        try {
            Thread.sleep(2000); // Give the seeder time to populate the DB.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        SessionManager.setCurrentUser(context, Staff.Role.ADMIN.name(), "Dummy@email.com");

    }

    @After
    public void tearDown() {
        Context context = ApplicationProvider.getApplicationContext();
        database = AppDatabase.getInstance(context);
        database.clearAllTables();

        SessionManager.clear(context);
    }

    public static ViewAction waitForView(final long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed(); // This action can be performed on any displayed view.
            }

            @Override
            public String getDescription() {
                return "Wait for a specific view to appear for " + millis + " milliseconds.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadForAtLeast(millis);
            }
        };
    }

    @Test
    public void createNewAppointment_isSuccessful() {

        // Arrange: Details for the new appointment
        String patientNhs = "6810564195";
        // Clinician name from the seeder data
        String clinicianToSelect = "Dr. Emily Carter";

        // Wait for the AppointmentListFragment to load and then click the FAB.
        onView(withId(R.id.appointmentContainer)).perform(waitForView(2000));
        onView(withId(R.id.fabBookAppointment)).perform(click());

        // Wait for the booking fragment to be displayed
        onView(withId(R.id.btnConfirmBooking)).check(matches(isDisplayed()));

        // Fill out the booking form
        onView(withId(R.id.etNhsBooking)).perform(typeText(patientNhs), closeSoftKeyboard());

        // Select Clinician
        onView(withId(R.id.spinnerClinicianBooking)).perform(click());
        Espresso.onData(equalTo(clinicianToSelect)).perform(click());

        // Set start time
        onView(withId(R.id.etStartMillis)).perform(click());
        onView(withText("OK")).perform(click()); // OK on date picker
        onView(withText("OK")).perform(click()); // OK on time picker

        // Set end time
        onView(withId(R.id.etEndMillis)).perform(click());
        onView(withText("OK")).perform(click()); // OK on date picker
        onView(withText("OK")).perform(click()); // OK on time picker

        // Click confirm
        onView(withId(R.id.btnConfirmBooking)).perform(click());

        // Assert: The fragment should pop back, and we should see the appointment list again.
        onView(ViewMatchers.withId(R.id.fabBookAppointment)).check(matches(isDisplayed()));
    }
}