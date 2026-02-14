package com.dmgproductions.amp.classifier.featureExtraction;

import com.dmgproductions.amp.gestures.Gesture;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class NormedGridExtractorTest {

    @Test
    public void testOutputIs64SamplesAndNormalized() {
        NormedGridExtractor extractor = new NormedGridExtractor();

        List<float[]> values = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            values.add(new float[]{
                    (float) Math.sin(i * 0.1) * 10,
                    (float) Math.cos(i * 0.1) * 10,
                    (float) (i * 0.5)
            });
        }
        Gesture input = new Gesture(values, "walking");

        Gesture result = extractor.sampleSignal(input);

        // Should be resampled to 64 points
        assertEquals(64, result.length());

        // Should be normalized to [0, 1] range
        for (int i = 0; i < result.length(); i++) {
            for (int j = 0; j < 3; j++) {
                float val = result.getValue(i, j);
                assertTrue("Value should be >= 0, got " + val, val >= -0.001f);
                assertTrue("Value should be <= 1, got " + val, val <= 1.001f);
            }
        }
    }

    @Test
    public void testLabelPreserved() {
        NormedGridExtractor extractor = new NormedGridExtractor();

        List<float[]> values = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            values.add(new float[]{(float) i, 0f, 0f});
        }
        Gesture input = new Gesture(values, "running");

        Gesture result = extractor.sampleSignal(input);
        assertEquals("running", result.getLabel());
    }

    @Test
    public void testImplementsIFeatureExtractorConstCount() {
        NormedGridExtractor extractor = new NormedGridExtractor();
        assertTrue(extractor instanceof IFeatureExtractorConstCount);
        assertTrue(extractor instanceof IFeatureExtractor);
    }
}
