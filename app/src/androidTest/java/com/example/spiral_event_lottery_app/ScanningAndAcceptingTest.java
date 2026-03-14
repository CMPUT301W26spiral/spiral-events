package com.example.spiral_event_lottery_app;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * this is for the UI tests for the QR Scanner feature of the Invitation Handler.
 */
@RunWith(AndroidJUnit4.class)
public class ScanningAndAcceptingTest {
    // we launching screen before actually testing
    @Rule
    public ActivityScenarioRule<QR_scanner> activity_rule =
            new ActivityScenarioRule<>(QR_scanner.class);
    /**
     * This is to see if the camera box actually showing up on the screen.
     */
    @Test
    public void test_camerabox_exists() {
        Espresso.onView(ViewMatchers.withId(R.id.camera_preview))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }
    /**
     * we are testing to see if the loading spinner is properly hidden when the screen opens and it is only for delay.
     */
    @Test
    public void test_ifspinner_hidden() {
        Espresso.onView(ViewMatchers.withId(R.id.loading_spinner))
                .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }
    /**
     * to test what happens if we pass null or empty data to firebase
     */
    @Test
    public void test_empty_data() {
        acceptanceHandling handler = new acceptanceHandling();
        try {
            // we passing null and empty strings to see if there is an issue or not
            handler.invitation_accepted(null, "", "");
        } catch (Exception e) {
            throw new AssertionError("crashed on empty database push due to no input.");
        }
    }
}