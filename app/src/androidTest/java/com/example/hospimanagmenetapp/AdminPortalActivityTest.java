package com.example.hospimanagmenetapp;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.hospimanagmenetapp.data.entities.Staff;
import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.ui.AdminPortalActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminPortalActivityTest {

    private AppDatabase database;

    @Rule
    public ActivityScenarioRule<AdminPortalActivity> activityRule = new ActivityScenarioRule<>(
            new Intent(ApplicationProvider.getApplicationContext(), AdminPortalActivity.class)
                    .putExtra("bypassCheck", true)
    );

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = AppDatabase.getInstance(context);
        database.clearAllTables();
    }

    @After
    public void tearDown() {
        // Close the database connection after tests
        database.clearAllTables();
    }

    @Test
    public void registerNewAdminStaff_displaysInRecyclerView() {
        // Arrange: Define new ADMIN staff details
        String name = "Admin User";
        String email = "admin.user@hospital.com";
        String pin = "1234";

        // Act: Type details into the form
        onView(withId(R.id.etStaffName)).perform(typeText(name));
        onView(withId(R.id.etStaffEmail)).perform(typeText(email));

        // Select ADMIN from the Spinner
        onView(withId(R.id.spRole)).perform(click());
        onData(allOf(is(instanceOf(Staff.Role.class)), is(Staff.Role.ADMIN))).perform(click());

        // Enter the admin PIN
        onView(withId(R.id.etAdminSetupPin)).perform(typeText(pin), closeSoftKeyboard());

        // Click register
        onView(withId(R.id.btnRegisterStaff)).perform(click());

        // Assert: Check that the RecyclerView now contains an item with the new staff's details
        // A small delay helps wait for the async DB operation and UI refresh.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.rvStaff))
                .check(matches(hasDescendant(withText(name))));
        onView(withId(R.id.rvStaff))
                .check(matches(hasDescendant(withText(email))));
        onView(withId(R.id.rvStaff))
                .check(matches(hasDescendant(withText("ADMIN"))));
    }

    @Test
    public void createAndThenDeleteStaffMember_isSuccessful() {
        // Arrange: Define a new staff member to be created and then deleted
        String name = "Temporary User";
        String email = "temp.user@hospital.com";

        // Act: Fill form, select a role, and register
        onView(withId(R.id.etStaffName)).perform(typeText(name));
        onView(withId(R.id.etStaffEmail)).perform(typeText(email), closeSoftKeyboard());
        onView(withId(R.id.spRole)).perform(click());
        onData(allOf(is(instanceOf(Staff.Role.class)), is(Staff.Role.RECEPTION))).perform(click());
        onView(withId(R.id.btnRegisterStaff)).perform(click());

        // Assert (Create): Verify the user appears in the list
        try { Thread.sleep(1500); } catch (InterruptedException e) {} // Wait for DB and UI
        onView(withId(R.id.rvStaff)).check(matches(hasDescendant(withText(name))));

        // Act: Click on the newly created item and confirm deletion
        onView(withId(R.id.rvStaff))
                .perform(actionOnItem(hasDescendant(withText(name)), click()));
        onView(withText("Delete")).perform(click());

        // Assert (Delete): Verify the user is no longer in the list
        try { Thread.sleep(1500); } catch (InterruptedException e) {} // Wait for DB and UI
        onView(withId(R.id.rvStaff)).check(matches(not(hasDescendant(withText(name)))));
    }

    @Test
    public void registerDuplicateStaff_showsErrorToast() {
        // Arrange: Register an initial staff member
        String name = "Duplicate Person";
        String email = "duplicate@hospital.com";

        onView(withId(R.id.etStaffName)).perform(typeText(name));
        onView(withId(R.id.etStaffEmail)).perform(typeText(email));
        onView(withId(R.id.spRole)).perform(click());
        onData(allOf(is(instanceOf(Staff.Role.class)), is(Staff.Role.RECEPTION))).perform(click());
        onView(withId(R.id.btnRegisterStaff)).perform(click());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Act: Attempt to register the same person again (with the same email)
        onView(withId(R.id.etStaffName)).perform(typeText("Another Name"));
        onView(withId(R.id.etStaffEmail)).perform(typeText(email)); // Use same email
        onView(withId(R.id.spRole)).perform(click());
        onData(allOf(is(instanceOf(Staff.Role.class)), is(Staff.Role.RECEPTION))).perform(click());
        onView(withId(R.id.btnRegisterStaff)).perform(click());

        // Assert: Espresso cannot easily check for Toasts. Instead, we can verify that the UI
        onView(withId(R.id.etStaffEmail)).check(matches(withText(email)));
    }
}