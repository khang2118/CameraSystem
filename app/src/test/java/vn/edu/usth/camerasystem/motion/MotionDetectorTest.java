package vn.edu.usth.camerasystem.motion;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class MotionDetectorTest {

    @Test
    public void detectsLocalChangesWithPaddedYPlane() {
        MotionDetector detector = new MotionDetector();
        assertFalse(detector.phatHien(image(false, false), 128, 96, 264, 2, 50));
        assertTrue(detector.phatHien(image(true, false), 128, 96, 264, 2, 50));
        assertEquals(0.5f, detector.getDiem(), 0.01f);
    }

    @Test
    public void ignoresGlobalLightChangesAndResetsPreviousFrame() {
        MotionDetector detector = new MotionDetector();
        assertFalse(detector.phatHien(image(false, false), 128, 96, 264, 2, 50));
        assertFalse(detector.phatHien(image(false, true), 128, 96, 264, 2, 50));
        detector.datLai();
        assertFalse(detector.phatHien(image(true, false), 128, 96, 264, 2, 50));
    }

    private ByteBuffer image(boolean localChange, boolean globalChange) {
        ByteBuffer buffer = ByteBuffer.allocate(264 * 96);
        for (int y = 0; y < 96; y++) {
            for (int x = 0; x < 128; x++) {
                buffer.put(y * 264 + x * 2, (byte) (globalChange || localChange && x < 64 ? 160 : 50));
            }
        }
        return buffer;
    }
}
