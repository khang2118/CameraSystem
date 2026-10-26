package vn.edu.usth.camerasystem.ui;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.TextView;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import vn.edu.usth.camerasystem.R;
import vn.edu.usth.camerasystem.ui.events.EventsFragment;
import vn.edu.usth.camerasystem.ui.monitor.MonitorFragment;
import vn.edu.usth.camerasystem.ui.settings.SettingsFragment;

/**
 * Shell Activity — bottom navigation + 3 tab Fragments (P1 Day 1–2).
 */
public class MainActivity extends AppCompatActivity {

    private static final String KEY_SELECTED_NAV = "selected_nav";
    private static final String PREFS = "camera_security_prefs";
    private static final String KEY_PIN = "pin";
    private static final String TAG_MONITOR = "tab_monitor";
    private static final String TAG_EVENTS = "tab_events";
    private static final String TAG_SETTINGS = "tab_settings";

    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNav;
    private View fragmentContainer;
    private View pinGate;
    private EditText pinInput;
    private TextView pinError;
    private int selectedNavId = R.id.nav_monitor;
    private boolean isUnlocked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return windowInsets;
        });
        boolean lightTheme = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES;
        WindowCompat.getInsetsController(getWindow(), root).setAppearanceLightStatusBars(lightTheme);
        WindowCompat.getInsetsController(getWindow(), root).setAppearanceLightNavigationBars(lightTheme);

        toolbar = findViewById(R.id.toolbar);
        bottomNav = findViewById(R.id.bottom_nav);
        fragmentContainer = findViewById(R.id.fragment_container);
        pinGate = findViewById(R.id.pin_gate);
        pinInput = findViewById(R.id.et_unlock_pin);
        pinError = findViewById(R.id.tv_pin_error);
        setSupportActionBar(toolbar);

        if (savedInstanceState != null) {
            selectedNavId = savedInstanceState.getInt(KEY_SELECTED_NAV, R.id.nav_monitor);
        }

        String savedPin = getSavedPin();
        isUnlocked = savedPin == null || Boolean.TRUE.equals(
                getLastCustomNonConfigurationInstance());
        if (isUnlocked) {
            showMainContent(savedInstanceState);
        } else {
            showPinGate(savedPin);
        }
    }

    private void showMainContent(Bundle savedInstanceState) {
        isUnlocked = true;
        pinGate.setVisibility(View.GONE);
        toolbar.setVisibility(View.VISIBLE);
        fragmentContainer.setVisibility(View.VISIBLE);
        bottomNav.setVisibility(View.VISIBLE);

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == selectedNavId
                    && getSupportFragmentManager().findFragmentById(R.id.fragment_container) != null) {
                return true;
            }
            selectedNavId = item.getItemId();
            return switchTo(selectedNavId);
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_monitor);
        } else {
            bottomNav.getMenu().findItem(selectedNavId).setChecked(true);
            updateToolbarTitle(selectedNavId);
        }
    }

    private void showPinGate(String savedPin) {
        toolbar.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.GONE);
        bottomNav.setVisibility(View.GONE);
        pinGate.setVisibility(View.VISIBLE);

        findViewById(R.id.btn_unlock_pin).setOnClickListener(view -> {
            String enteredPin = pinInput.getText().toString().trim();
            if (savedPin.equals(enteredPin)) {
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(pinInput.getWindowToken(), 0);
                showMainContent(null);
            } else {
                pinError.setVisibility(View.VISIBLE);
                pinInput.setText("");
                pinInput.setError(getString(R.string.pin_gate_error));
                pinInput.requestFocus();
            }
        });

        pinInput.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                findViewById(R.id.btn_unlock_pin).performClick();
                return true;
            }
            return false;
        });
    }

    private String getSavedPin() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String pin = prefs.getString(KEY_PIN, null);
        return pin == null || pin.isEmpty() ? null : pin;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_NAV, selectedNavId);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return isUnlocked;
    }

    private boolean switchTo(int itemId) {
        String tag;
        if (itemId == R.id.nav_events) {
            tag = TAG_EVENTS;
        } else if (itemId == R.id.nav_settings) {
            tag = TAG_SETTINGS;
        } else {
            tag = TAG_MONITOR;
            itemId = R.id.nav_monitor;
        }

        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        Fragment monitor = manager.findFragmentByTag(TAG_MONITOR);
        Fragment events = manager.findFragmentByTag(TAG_EVENTS);
        Fragment settings = manager.findFragmentByTag(TAG_SETTINGS);

        if (TAG_EVENTS.equals(tag)) {
            if (events == null) {
                transaction.add(R.id.fragment_container, new EventsFragment(), TAG_EVENTS);
            } else {
                transaction.show(events);
            }
            if (monitor != null) transaction.hide(monitor);
            if (settings != null) transaction.hide(settings);
        } else if (TAG_SETTINGS.equals(tag)) {
            if (settings == null) {
                transaction.add(R.id.fragment_container, new SettingsFragment(), TAG_SETTINGS);
            } else {
                transaction.show(settings);
            }
            if (monitor != null) transaction.hide(monitor);
            if (events != null) transaction.hide(events);
        } else {
            if (monitor == null) {
                transaction.add(R.id.fragment_container, new MonitorFragment(), TAG_MONITOR);
            } else {
                transaction.show(monitor);
            }
            if (events != null) transaction.hide(events);
            if (settings != null) transaction.hide(settings);
        }

        transaction.commit();
        updateToolbarTitle(itemId);
        return true;
    }

    private void updateToolbarTitle(int itemId) {
        if (itemId == R.id.nav_events) {
            toolbar.setTitle(R.string.nav_events);
        } else if (itemId == R.id.nav_settings) {
            toolbar.setTitle(R.string.nav_settings);
        } else {
            toolbar.setTitle(R.string.nav_monitor);
        }
    }
}
