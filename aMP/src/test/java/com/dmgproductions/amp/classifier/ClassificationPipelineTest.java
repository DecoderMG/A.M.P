package com.dmgproductions.amp.classifier;

import com.dmgproductions.amp.classifier.featureExtraction.NormedGridExtractor;
import com.dmgproductions.amp.gestures.Gesture;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration test for the full classification pipeline:
 * Raw gesture -> Feature extraction -> DTW comparison -> Distribution result
 */
public class ClassificationPipelineTest {

    private Gesture createWalkingGesture() {
        List<float[]> values = new ArrayList<>();
        // Simulate walking: periodic low-amplitude acceleration pattern
        for (int i = 0; i < 100; i++) {
            float x = (float) (Math.sin(i * 0.3) * 3.0);
            float y = (float) (Math.cos(i * 0.3) * 2.0);
            float z = 9.8f + (float) (Math.sin(i * 0.6) * 1.5);
            values.add(new float[]{x, y, z});
        }
        return new Gesture(values, "walking");
    }

    private Gesture createRunningGesture() {
        List<float[]> values = new ArrayList<>();
        // Simulate running: periodic high-amplitude acceleration pattern (faster frequency)
        for (int i = 0; i < 100; i++) {
            float x = (float) (Math.sin(i * 0.6) * 8.0);
            float y = (float) (Math.cos(i * 0.6) * 6.0);
            float z = 9.8f + (float) (Math.sin(i * 1.2) * 5.0);
            values.add(new float[]{x, y, z});
        }
        return new Gesture(values, "running");
    }

    @Test
    public void testFeatureExtractionProduces64Samples() {
        NormedGridExtractor extractor = new NormedGridExtractor();

        Gesture walking = createWalkingGesture();
        Gesture sampled = extractor.sampleSignal(walking);

        assertEquals(64, sampled.length());
    }

    @Test
    public void testWalkingMatchesWalkingBetterThanRunning() {
        NormedGridExtractor extractor = new NormedGridExtractor();

        Gesture walkingTraining = createWalkingGesture();
        Gesture runningTraining = createRunningGesture();

        // Create a test walking gesture (slightly different from training)
        List<float[]> testValues = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            float x = (float) (Math.sin(i * 0.3 + 0.1) * 3.2);
            float y = (float) (Math.cos(i * 0.3 + 0.1) * 2.1);
            float z = 9.8f + (float) (Math.sin(i * 0.6 + 0.1) * 1.6);
            testValues.add(new float[]{x, y, z});
        }
        Gesture testWalking = new Gesture(testValues, null);

        // Extract features
        Gesture sampledTrainingWalk = extractor.sampleSignal(walkingTraining);
        Gesture sampledTrainingRun = extractor.sampleSignal(runningTraining);
        Gesture sampledTest = extractor.sampleSignal(testWalking);

        // Compare distances
        float distToWalking = DTWAlgorithm.calcDistance(sampledTrainingWalk, sampledTest);
        float distToRunning = DTWAlgorithm.calcDistance(sampledTrainingRun, sampledTest);

        assertTrue("Walking test sample should be closer to walking training than running",
                distToWalking < distToRunning);
    }

    @Test
    public void testDistributionFromMultipleTrainingSamples() {
        NormedGridExtractor extractor = new NormedGridExtractor();

        // Create multiple training samples
        Gesture walk1 = extractor.sampleSignal(createWalkingGesture());
        Gesture run1 = extractor.sampleSignal(createRunningGesture());

        // Create a test gesture similar to walking
        List<float[]> testValues = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            float x = (float) (Math.sin(i * 0.3 + 0.2) * 3.1);
            float y = (float) (Math.cos(i * 0.3 + 0.2) * 2.2);
            float z = 9.8f + (float) (Math.sin(i * 0.6 + 0.2) * 1.4);
            testValues.add(new float[]{x, y, z});
        }
        Gesture testGesture = extractor.sampleSignal(new Gesture(testValues, null));

        // Build distribution manually
        Distribution distribution = new Distribution();
        distribution.addEntry("walking", DTWAlgorithm.calcDistance(walk1, testGesture));
        distribution.addEntry("running", DTWAlgorithm.calcDistance(run1, testGesture));

        assertEquals("walking", distribution.getBestMatch());
        assertEquals(2, distribution.size());
        assertTrue(distribution.getBestDistance() >= 0);
    }

    @Test
    public void testRunningMatchesRunningBetterThanWalking() {
        NormedGridExtractor extractor = new NormedGridExtractor();

        Gesture walkingTraining = extractor.sampleSignal(createWalkingGesture());
        Gesture runningTraining = extractor.sampleSignal(createRunningGesture());

        // Create a test running gesture
        List<float[]> testValues = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            float x = (float) (Math.sin(i * 0.6 + 0.15) * 7.8);
            float y = (float) (Math.cos(i * 0.6 + 0.15) * 5.8);
            float z = 9.8f + (float) (Math.sin(i * 1.2 + 0.15) * 4.8);
            testValues.add(new float[]{x, y, z});
        }
        Gesture testRunning = extractor.sampleSignal(new Gesture(testValues, null));

        float distToWalking = DTWAlgorithm.calcDistance(walkingTraining, testRunning);
        float distToRunning = DTWAlgorithm.calcDistance(runningTraining, testRunning);

        assertTrue("Running test sample should be closer to running training than walking",
                distToRunning < distToWalking);
    }
}
