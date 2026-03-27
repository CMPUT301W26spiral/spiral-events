package com.example.spiral_event_lottery_app;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.spiral_event_lottery_app.ui.admin.AdminFragment;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * UI tests for AdminFragment covering browsing and navigation between tabs.
 * Tests US 03.04.01 (Browse Events), US 03.05.01 (Browse Profiles), US 03.06.01 (Browse Images).
 */
@RunWith(AndroidJUnit4.class)
public class AdminFragmentTest {

    /**
     * Tests that the admin panel loads and all browse buttons are visible and clickable.
     */
    @Test
    public void testAdminPanelButtonsVisible() {
        FragmentScenario.launchInContainer(AdminFragment.class);

        Espresso.onView(ViewMatchers.withId(R.id.btn_admin_events))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click());

        Espresso.onView(ViewMatchers.withId(R.id.btn_admin_profiles))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click());

        Espresso.onView(ViewMatchers.withId(R.id.btn_admin_images))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click());

        Espresso.onView(ViewMatchers.withId(R.id.admin_recycler_view))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    /**
     * Tests that notification logs and comments tabs are accessible.
     * Covers US 03.08.01 and US 03.10.01.
     */
    @Test
    public void testAdminLogsAndCommentsTabs() {
        FragmentScenario.launchInContainer(AdminFragment.class);

        Espresso.onView(ViewMatchers.withId(R.id.btn_admin_notif_logs))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click());

        Espresso.onView(ViewMatchers.withId(R.id.btn_admin_comments))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click());
    }
}