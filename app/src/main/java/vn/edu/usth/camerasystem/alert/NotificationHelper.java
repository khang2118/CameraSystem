package vn.edu.usth.camerasystem.alert;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import vn.edu.usth.camerasystem.R;
import vn.edu.usth.camerasystem.ui.MainActivity;

/** Tạo kênh và hiển thị thông báo phát hiện chuyển động. */
public final class NotificationHelper {

    public static final String CHANNEL_ID = "motion_alert_channel";
    private static int notificationId = 1000;

    private NotificationHelper() { /* utility */ }

    /** Tạo kênh thông báo trên Android 8.0 trở lên. */
    public static void createChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.notification_channel_name);
            String desc = context.getString(R.string.notification_channel_desc);
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(desc);
            channel.enableVibration(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /** Hiển thị thông báo và mở màn hình chính khi người dùng chạm vào. */
    public static void show(@NonNull Context context, @NonNull String title, @NonNull String body) {
        // Intent khi nhấn notification → mở MainActivity
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_nav_monitor)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND);

        try {
            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
            managerCompat.notify(notificationId++, builder.build());
        } catch (SecurityException e) {
            android.util.Log.w("NotificationHelper", "Cannot post notification — permission denied", e);
        }
    }
}

