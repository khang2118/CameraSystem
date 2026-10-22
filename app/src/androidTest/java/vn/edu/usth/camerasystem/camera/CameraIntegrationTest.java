package vn.edu.usth.camerasystem.camera;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;

import androidx.camera.view.PreviewView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import vn.edu.usth.camerasystem.R;
import vn.edu.usth.camerasystem.ui.AppController;
import vn.edu.usth.camerasystem.ui.MainActivity;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class CameraIntegrationTest {

    @Before
    public void preparePermission() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("camera_security_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("armed", false).commit();
        ParcelFileDescriptor descriptor = InstrumentationRegistry.getInstrumentation()
                .getUiAutomation().executeShellCommand("pm grant " + context.getPackageName()
                        + " android.permission.CAMERA");
        try (ParcelFileDescriptor.AutoCloseInputStream stream =
                     new ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
            while (stream.read() != -1) { }
        }
    }

    @Test
    public void previewAnalysisSwitchAndLifecycle() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            CameraController camera = AppController.get().getCameraController();
            awaitPreview(scenario);
            for (int index = 0; index < 4; index++) {
                scenario.onActivity(activity -> {
                    int previous = camera.getLensFacing();
                    assertTrue(camera.switchCamera());
                    assertNotEquals(previous, camera.getLensFacing());
                });
                awaitPreview(scenario);
            }
            for (int index = 0; index < 3; index++) {
                selectTab(scenario, R.id.nav_events);
                awaitCondition(() -> !readReady(camera));
                selectTab(scenario, R.id.nav_monitor);
                awaitPreview(scenario);
            }
            scenario.moveToState(Lifecycle.State.CREATED);
            assertFalse(readReady(camera));
            scenario.moveToState(Lifecycle.State.RESUMED);
            awaitPreview(scenario);
            scenario.recreate();
            awaitPreview(scenario);
            scenario.onActivity(activity -> {
                if (!camera.hasTorch()) {
                    assertFalse(camera.toggleTorch());
                    assertFalse(activity.findViewById(R.id.btn_camera_torch).isEnabled());
                }
            });
        }
    }

    @Test
    public void recordingKeepsFramesAndRestartWaitsForFinalize() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AppController app = AppController.get();
            CameraController camera = app.getCameraController();
            awaitPreview(scenario);
            Set<String> previous = videoNames();
            scenario.onActivity(activity -> {
                assertTrue(app.startRecording());
                assertFalse(app.startRecording());
                assertFalse(camera.switchCamera());
            });
            awaitVideoData(previous);
            long frames = camera.getFrameCount();
            awaitCondition(() -> camera.getFrameCount() >= frames + 10);
            RecordingResult result = new RecordingResult();
            scenario.onActivity(activity -> {
                app.stopRecording(result);
                camera.stop();
                Fragment fragment = activity.getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_container);
                assertNotNull(fragment);
                camera.start(fragment.getViewLifecycleOwner(), activity.findViewById(R.id.preview_view),
                        (CameraController.Listener) fragment);
            });
            result.verify();
            awaitPreview(scenario);
        }
    }

    @Test
    public void motionClipIsSavedOnExitAndOldTimerDoesNotStopNextClip() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AppController app = AppController.get();
            awaitPreview(scenario);
            int eventCount = app.getEventRepository().getAllEvents().size();
            Set<String> previous = videoNames();
            long started = SystemClock.elapsedRealtime();
            scenario.onActivity(activity -> {
                app.getAlertManager().setArmed(true);
                app.onMotionDetected(0.4f, System.currentTimeMillis());
                assertTrue(app.getRecordingController().isRecording());
                app.getAlertManager().setArmed(false);
            });
            awaitVideoData(previous);
            selectTab(scenario, R.id.nav_events);
            awaitCondition(() -> app.getEventRepository().getAllEvents().size() == eventCount + 1);
            String motionPath = app.getEventRepository().getAllEvents().get(0).getVideoPath();
            verifyVideo(motionPath);
            selectTab(scenario, R.id.nav_monitor);
            awaitPreview(scenario);
            scenario.onActivity(activity -> assertTrue(app.startRecording()));
            awaitCondition(() -> SystemClock.elapsedRealtime() - started >= 10_000);
            assertTrue(app.getRecordingController().isRecording());
            RecordingResult result = new RecordingResult();
            scenario.onActivity(activity -> app.stopRecording(result));
            result.verify();
            assertEquals(eventCount + 1, app.getEventRepository().getAllEvents().size());
        }
    }

    private void selectTab(ActivityScenario<MainActivity> scenario, int id) {
        scenario.onActivity(activity -> ((BottomNavigationView) activity.findViewById(R.id.bottom_nav))
                .setSelectedItemId(id));
    }

    private void awaitPreview(ActivityScenario<MainActivity> scenario) throws Exception {
        awaitCondition(() -> {
            AtomicReference<Boolean> streaming = new AtomicReference<>(false);
            scenario.onActivity(activity -> {
                PreviewView preview = activity.findViewById(R.id.preview_view);
                CameraController camera = AppController.get().getCameraController();
                streaming.set(camera.isReady() && camera.getFrameCount() >= 10
                        && preview.getPreviewStreamState().getValue() == PreviewView.StreamState.STREAMING);
            });
            return streaming.get();
        });
    }

    private boolean readReady(CameraController camera) {
        AtomicReference<Boolean> ready = new AtomicReference<>(false);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> ready.set(camera.isReady()));
        return ready.get();
    }

    private File videoDirectory() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("Videos");
    }

    private Set<String> videoNames() {
        Set<String> names = new HashSet<>();
        File[] files = videoDirectory().listFiles();
        if (files != null) for (File file : files) names.add(file.getName());
        return names;
    }

    private void awaitVideoData(Set<String> previous) throws Exception {
        awaitCondition(() -> {
            File[] files = videoDirectory().listFiles();
            if (files == null) return false;
            for (File file : files) {
                if (file.getName().endsWith(".mp4") && !previous.contains(file.getName())
                        && file.length() > 1024) return true;
            }
            return false;
        });
    }

    private void verifyVideo(String path) throws Exception {
        assertNotNull(path);
        File video = new File(path);
        assertTrue(video.length() > 0);
        assertTrue(new File(path.replace(".mp4", ".json")).isFile());
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            assertTrue(Long.parseLong(retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION)) > 0);
        } finally {
            retriever.release();
        }
    }

    private void awaitCondition(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) return;
            Thread.sleep(100);
        }
        fail("Camera condition was not reached within 45 seconds");
    }

    private class RecordingResult implements RecordingController.Callback {
        private final CountDownLatch finished = new CountDownLatch(1);
        private String path;
        private String error;

        @Override
        public void onRecordingSaved(String filePath) {
            path = filePath;
            finished.countDown();
        }

        @Override
        public void onRecordingFailed(String reason) {
            error = reason;
            finished.countDown();
        }

        void verify() throws Exception {
            assertTrue("Video did not finalize", finished.await(45, TimeUnit.SECONDS));
            assertNull(error);
            verifyVideo(path);
        }
    }
}
