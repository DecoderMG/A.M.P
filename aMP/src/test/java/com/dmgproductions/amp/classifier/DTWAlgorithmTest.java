package com.dmgproductions.amp.classifier;

import com.dmgproductions.amp.gestures.Gesture;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class DTWAlgorithmTest {

    private Gesture createGesture(float[][] data, String label) {
        List<float[]> values = new ArrayList<>();
        for (float[] datum : data) {
            values.add(datum);
        }
        return new Gesture(values, label);
    }

    @Test
    public void testIdenticalSignalsHaveZeroDistance() {
        float[][] data = {
                {1.0f, 0.0f, 0.0f},
                {0.0f, 1.0f, 0.0f},
                {0.0f, 0.0f, 1.0f}
        };
        Gesture a = createGesture(data, "test");
        Gesture b = createGesture(data, "test");

        float distance = DTWAlgorithm.calcDistance(a, b);
        assertEquals(0.0f, distance, 0.001f);
    }

    @Test
    public void testDifferentSignalsHavePositiveDistance() {
        float[][] dataA = {
                {1.0f, 0.0f, 0.0f},
                {2.0f, 0.0f, 0.0f},
                {3.0f, 0.0f, 0.0f}
        };
        float[][] dataB = {
                {5.0f, 0.0f, 0.0f},
                {6.0f, 0.0f, 0.0f},
                {7.0f, 0.0f, 0.0f}
        };
        Gesture a = createGesture(dataA, "a");
        Gesture b = createGesture(dataB, "b");

        float distance = DTWAlgorithm.calcDistance(a, b);
        assertTrue("Distance should be positive for different signals", distance > 0);
    }

    @Test
    public void testSimilarSignalsCloserThanDissimilar() {
        float[][] dataA = {
                {1.0f, 1.0f, 1.0f},
                {2.0f, 2.0f, 2.0f},
                {3.0f, 3.0f, 3.0f}
        };
        float[][] dataSimilar = {
                {1.1f, 1.1f, 1.1f},
                {2.1f, 2.1f, 2.1f},
                {3.1f, 3.1f, 3.1f}
        };
        float[][] dataDissimilar = {
                {10.0f, 10.0f, 10.0f},
                {20.0f, 20.0f, 20.0f},
                {30.0f, 30.0f, 30.0f}
        };

        Gesture a = createGesture(dataA, "a");
        Gesture similar = createGesture(dataSimilar, "similar");
        Gesture dissimilar = createGesture(dataDissimilar, "dissimilar");

        float distSimilar = DTWAlgorithm.calcDistance(a, similar);
        float distDissimilar = DTWAlgorithm.calcDistance(a, dissimilar);

        assertTrue("Similar signal should have lower DTW distance than dissimilar",
                distSimilar < distDissimilar);
    }

    @Test
    public void testDTWIsSymmetric() {
        float[][] dataA = {
                {1.0f, 2.0f, 3.0f},
                {4.0f, 5.0f, 6.0f}
        };
        float[][] dataB = {
                {7.0f, 8.0f, 9.0f},
                {10.0f, 11.0f, 12.0f}
        };

        Gesture a = createGesture(dataA, "a");
        Gesture b = createGesture(dataB, "b");

        float distAB = DTWAlgorithm.calcDistance(a, b);
        float distBA = DTWAlgorithm.calcDistance(b, a);

        assertEquals("DTW distance should be symmetric", distAB, distBA, 0.01f);
    }

    @Test
    public void testDifferentLengthSignals() {
        float[][] dataShort = {
                {1.0f, 0.0f, 0.0f},
                {2.0f, 0.0f, 0.0f}
        };
        float[][] dataLong = {
                {1.0f, 0.0f, 0.0f},
                {1.5f, 0.0f, 0.0f},
                {2.0f, 0.0f, 0.0f},
                {2.5f, 0.0f, 0.0f}
        };

        Gesture shortG = createGesture(dataShort, "short");
        Gesture longG = createGesture(dataLong, "long");

        float distance = DTWAlgorithm.calcDistance(shortG, longG);
        assertTrue("DTW should handle different length signals", distance >= 0);
    }

    @Test
    public void testTimeShiftedSignalsLowDistance() {
        float[][] dataA = {
                {0.0f, 0.0f, 0.0f},
                {1.0f, 1.0f, 1.0f},
                {2.0f, 2.0f, 2.0f},
                {1.0f, 1.0f, 1.0f},
                {0.0f, 0.0f, 0.0f}
        };
        float[][] dataB = {
                {0.0f, 0.0f, 0.0f},
                {0.0f, 0.0f, 0.0f},
                {1.0f, 1.0f, 1.0f},
                {2.0f, 2.0f, 2.0f},
                {1.0f, 1.0f, 1.0f}
        };
        float[][] dataC = {
                {5.0f, 5.0f, 5.0f},
                {10.0f, 10.0f, 10.0f},
                {15.0f, 15.0f, 15.0f},
                {10.0f, 10.0f, 10.0f},
                {5.0f, 5.0f, 5.0f}
        };

        Gesture a = createGesture(dataA, "a");
        Gesture b = createGesture(dataB, "b");
        Gesture c = createGesture(dataC, "c");

        float distAB = DTWAlgorithm.calcDistance(a, b);
        float distAC = DTWAlgorithm.calcDistance(a, c);

        assertTrue("Time-shifted similar signal should have lower distance than different signal",
                distAB < distAC);
    }
}
