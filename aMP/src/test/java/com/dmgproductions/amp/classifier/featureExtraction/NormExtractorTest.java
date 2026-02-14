package com.dmgproductions.amp.classifier.featureExtraction;

import com.dmgproductions.amp.gestures.Gesture;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class NormExtractorTest {

    @Test
    public void testOutputNormalizedTo01Range() {
        NormExtractor extractor = new NormExtractor();

        List<float[]> values = new ArrayList<>();
        values.add(new float[]{-5.0f, 0.0f, 10.0f});
        values.add(new float[]{0.0f, 5.0f, 0.0f});
        values.add(new float[]{5.0f, 10.0f, -5.0f});
        Gesture input = new Gesture(values, "test");

        Gesture result = extractor.sampleSignal(input);

        for (int i = 0; i < result.length(); i++) {
            for (int j = 0; j < 3; j++) {
                float val = result.getValue(i, j);
                assertTrue("Normalized value should be >= 0, got " + val, val >= -0.001f);
                assertTrue("Normalized value should be <= 1, got " + val, val <= 1.001f);
            }
        }
    }

    @Test
    public void testMinValueBecomesZero() {
        NormExtractor extractor = new NormExtractor();

        List<float[]> values = new ArrayList<>();
        values.add(new float[]{-10.0f, 0.0f, 5.0f});
        values.add(new float[]{0.0f, 5.0f, 10.0f});
        values.add(new float[]{10.0f, 10.0f, 10.0f});
        Gesture input = new Gesture(values, "test");

        Gesture result = extractor.sampleSignal(input);

        // The global minimum across all dims is -10.0, so the sample at (0,0) should be 0
        assertEquals(0.0f, result.getValue(0, 0), 0.001f);
    }

    @Test
    public void testMaxValueBecomesOne() {
        NormExtractor extractor = new NormExtractor();

        List<float[]> values = new ArrayList<>();
        values.add(new float[]{-10.0f, 0.0f, 5.0f});
        values.add(new float[]{0.0f, 5.0f, 10.0f});
        values.add(new float[]{10.0f, 10.0f, 10.0f});
        Gesture input = new Gesture(values, "test");

        Gesture result = extractor.sampleSignal(input);

        // The global max is 10.0, so values at 10.0 should become 1.0
        assertEquals(1.0f, result.getValue(2, 0), 0.001f);
    }

    @Test
    public void testLabelPreserved() {
        NormExtractor extractor = new NormExtractor();

        List<float[]> values = new ArrayList<>();
        values.add(new float[]{1.0f, 2.0f, 3.0f});
        values.add(new float[]{4.0f, 5.0f, 6.0f});
        Gesture input = new Gesture(values, "running");

        Gesture result = extractor.sampleSignal(input);
        assertEquals("running", result.getLabel());
    }

    @Test
    public void testLengthPreserved() {
        NormExtractor extractor = new NormExtractor();

        List<float[]> values = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            values.add(new float[]{(float) i, (float) (i * 2), (float) (i * 3)});
        }
        Gesture input = new Gesture(values, "test");

        Gesture result = extractor.sampleSignal(input);
        assertEquals(50, result.length());
    }
}
