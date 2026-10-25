package vn.edu.usth.camerasystem;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import vn.edu.usth.camerasystem.alert.NotificationHelper;
import vn.edu.usth.camerasystem.ui.MainActivity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class P6SettingsAndAlertTest {

    private static final String PREFS = "camera_security_prefs";
    private static final String KEY_PIN = "pin";

    private Context appContext;
    private ActivityScenario<MainActivity> scenario;

    @Before
    public void setUp() {
        appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        scenario = ActivityScenario.launch(MainActivity.class);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        NotificationManager manager = appContext.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.cancelAll();
        }
    }

    @Test
    public void pinCanBeSetChangedAndUsedToUnlock() {
        scenario.onActivity(activity -> {
            ((BottomNavigationView) activity.findViewById(R.id.bottom_nav))
                    .setSelectedItemId(R.id.nav_settings);
            activity.getSupportFragmentManager().executePendingTransactions();

            EditText newPin = activity.findViewById(R.id.et_pin);
            EditText currentPin = activity.findViewById(R.id.et_current_pin);
            View saveButton = activity.findViewById(R.id.btn_save_pin);
            TextView status = activity.findViewById(R.id.tv_pin_status);

            newPin.setText("1234");
            saveButton.performClick();
            assertEquals("PIN đã đặt (4 chữ số)", status.getText().toString());

            currentPin.setText("9999");
            newPin.setText("5678");
            saveButton.performClick();
            assertEquals("PIN hiện tại không đúng", currentPin.getError().toString());
            assertPinEquals("1234");

            currentPin.setText("1234");
            saveButton.performClick();
        });
        assertPinEquals("5678");

        scenario.close();
        scenario = ActivityScenario.launch(MainActivity.class);
        scenario.onActivity(activity -> {
            assertEquals(View.VISIBLE, activity.findViewById(R.id.pin_gate).getVisibility());
            EditText unlockPin = activity.findViewById(R.id.et_unlock_pin);
            View unlockButton = activity.findViewById(R.id.btn_unlock_pin);
            View error = activity.findViewById(R.id.tv_pin_error);

            unlockPin.setText("1234");
            unlockButton.performClick();
            assertEquals(View.VISIBLE, error.getVisibility());

            unlockPin.setText("5678");
            unlockButton.performClick();
            assertEquals(View.VISIBLE, activity.findViewById(R.id.bottom_nav).getVisibility());
        });
    }

    @Test
    public void notificationHelperPostsMotionAlert() {
        grantNotificationPermissionIfNeeded();

        NotificationHelper.show(appContext, "P6 test alert", "Motion clip saved");

        NotificationManager manager = appContext.getSystemService(NotificationManager.class);
        assertNotNull("Notification manager is unavailable", manager);
        boolean found = false;
        for (android.service.notification.StatusBarNotification posted : manager.getActiveNotifications()) {
            Notification notification = posted.getNotification();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                    || NotificationHelper.CHANNEL_ID.equals(notification.getChannelId())) {
                found = true;
                break;
            }
        }
        assertTrue("Motion alert was not posted", found);
    }

    private void assertPinEquals(String expected) {
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        assertEquals(expected, prefs.getString(KEY_PIN, null));
    }

    private void grantNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .grantRuntimePermission(appContext.getPackageName(), Manifest.permission.POST_NOTIFICATIONS);
        }
    }
}
