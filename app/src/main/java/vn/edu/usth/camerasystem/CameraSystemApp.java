package vn.edu.usth.camerasystem;

import android.app.Application;

import vn.edu.usth.camerasystem.ui.AppController;

/**
 * Application entry — khởi tạo AppController (interfaces + stub Day 3).
 */
public class CameraSystemApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppController.init(this);
    }
}
