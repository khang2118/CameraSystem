package vn.edu.usth.camerasystem;

import android.app.Application;

import vn.edu.usth.camerasystem.alert.NotificationHelper;
import vn.edu.usth.camerasystem.ui.AppController;

/** Khởi tạo các thành phần dùng chung khi app mở. */
public class CameraSystemApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppController.init(this);

        NotificationHelper.createChannel(this);
    }
}
