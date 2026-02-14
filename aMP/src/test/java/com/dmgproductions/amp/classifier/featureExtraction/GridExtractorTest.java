package com.dmgproductions.amp.classifier.featureExtraction;

import com.dmgproductions.amp.gestures.Gesture;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class GridExtractorTest {

    private Gesture createGesture(int length, String label) {
        List<float[]> values = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            values.add(new float[]{(float) i, (float) (i * 2), (float) (i * 3)});
        }
        return new Gesture(values, label);
    }

    @Test
    public void testOutputAlways64Samples() {
        GridExtractor extractor = new GridExtractor();

        // Test with various input sizes
        int[] testSizes = {65, 100, 200, 500};
        for (int size : testSizes) {
            Gesture input = createGesture(size, "test");
            Gesture result = extractor.sampleSignal(input);
            assertEquals("Output should always have 64 samples for input size " + size,
                    64, result.length());
        }
    }

    @Test
    public void testLabelPreserved() {
        GridExtractor extractor = new GridExtractor();
        Gesture input = createGesture(100, "walking");
        Gesture result = extractor.sampleSignal(input);
        assertEquals("walking", result.getLabel());
    }

    @Test
    public void testFirstAndLastValuesPreserved() {
        GridExtractor extractor = new GridExtractor();
        Gesture input = createGesture(100, "test");
        Gesture result = extractor.sampleSignal(input);

        // First value should match input's first value
        assertEquals(input.getValue(0, 0), result.getValue(0, 0), 0.1f);
    }

    @Test
    public void testOutputHas3Dimensions() {
        GridExtractor extractor = new GridExtractor();
        Gesture input = createGesture(100, "test");
        Gesture result = extractor.sampleSignal(input);

        for (int i = 0; i < result.length(); i++) {
            float[] values = result.getValues().get(i);
            assertEquals("Each sample should have 3 dimensions", 3, values.length);
        }
    }

    @Test
    public void testMonotonicallyIncreasingInputProducesMonotonicOutput() {
        GridExtractor extractor = new GridExtractor();
        Gesture input = createGesture(200, "test");
        Gesture result = extractor.sampleSignal(input);

        // For monotonically increasing input, output X axis should also be increasing
        for (int i = 1; i < result.length(); i++) {
            assertTrue("Output should be monotonically increasing for X axis",
                    result.getValue(i, 0) >= result.getValue(i - 1, 0));
        }
    }
}
