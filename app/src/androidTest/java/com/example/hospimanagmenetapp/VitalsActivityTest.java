package com.example.hospimanagmenetapp;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.feature.ehr.ui.VitalsActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class VitalsActivityTest {

    private static final String TEST_NHS_NUMBER = "1234567890";

    @Rule
    public ActivityScenarioRule<VitalsActivity> activityRule = new ActivityScenarioRule<>(
            new Intent(ApplicationProvider.getApplicationContext(), VitalsActivity.class)
                    .putExtra("nhsNumber", TEST_NHS_NUMBER)
    );

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        AppDatabase database = AppDatabase.getInstance(context);
        database.clearAllTables();
    }

    @After
    public void tearDown() {
        Context context = ApplicationProvider.getApplicationContext();
        AppDatabase database = AppDatabase.getInstance(context);
        database.clearAllTables();
    }

    @Test
    public void saveVitals_displaysInList() {
        // Vitals data
        String temp = "37.5";
        String heartRate = "80";
        String systolic = "120";
        String diastolic = "80";

        // Fill in the form and save
        onView(withId(R.id.etTemperature)).perform(typeText(temp));
        onView(withId(R.id.etHeartRate)).perform(typeText(heartRate));
        onView(withId(R.id.etSystolic)).perform(typeText(systolic));
        onView(withId(R.id.etDiastolic)).perform(typeText(diastolic), closeSoftKeyboard());
        onView(withId(R.id.btnSaveVitals)).perform(click());

        // Wait for UI to refresh
        try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }

        // Assert: The saved vitals should be visible in the RecyclerView.
        onView(withText("BP: 120/80 mmHg | HR: 80 bpm | Temp: 37.5°C")).check(matches(isDisplayed()));
    }

    @Test
    public void paginationButtons_initialState() {
        // Assert: On initial load with no data, both buttons should be disabled.
        // The page info should show "Page 1 of 1".
        onView(withId(R.id.tvPageInfo)).check(matches(withText("Page 1 of 1")));
        onView(withId(R.id.btnPreviousPage)).check(matches(not(isEnabled())));
        onView(withId(R.id.btnNextPage)).check(matches(not(isEnabled())));
    }
}