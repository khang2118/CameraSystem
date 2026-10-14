package vn.edu.usth.camerasystem.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

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

    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNav;
    private int selectedNavId = R.id.nav_monitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        bottomNav = findViewById(R.id.bottom_nav);
        setSupportActionBar(toolbar);

        if (savedInstanceState != null) {
            selectedNavId = savedInstanceState.getInt(KEY_SELECTED_NAV, R.id.nav_monitor);
        }

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

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_NAV, selectedNavId);
    }

    private boolean switchTo(int itemId) {
        Fragment fragment;
        if (itemId == R.id.nav_events) {
            fragment = new EventsFragment();
        } else if (itemId == R.id.nav_settings) {
            fragment = new SettingsFragment();
        } else {
            fragment = new MonitorFragment();
            itemId = R.id.nav_monitor;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();

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
