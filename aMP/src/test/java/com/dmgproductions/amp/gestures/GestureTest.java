package com.dmgproductions.amp.gestures;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class GestureTest {

    private Gesture gesture;
    private List<float[]> testValues;

    @Before
    public void setUp() {
        testValues = new ArrayList<>();
        testValues.add(new float[]{1.0f, 2.0f, 3.0f});
        testValues.add(new float[]{4.0f, 5.0f, 6.0f});
        testValues.add(new float[]{7.0f, 8.0f, 9.0f});
        gesture = new Gesture(testValues, "walking");
    }

    @Test
    public void testGetLabel() {
        assertEquals("walking", gesture.getLabel());
    }

    @Test
    public void testSetLabel() {
        gesture.setLabel("running");
        assertEquals("running", gesture.getLabel());
    }

    @Test
    public void testLength() {
        assertEquals(3, gesture.length());
    }

    @Test
    public void testGetValue() {
        assertEquals(1.0f, gesture.getValue(0, 0), 0.001f);
        assertEquals(5.0f, gesture.getValue(1, 1), 0.001f);
        assertEquals(9.0f, gesture.getValue(2, 2), 0.001f);
    }

    @Test
    public void testSetValue() {
        gesture.setValue(0, 0, 99.0f);
        assertEquals(99.0f, gesture.getValue(0, 0), 0.001f);
    }

    @Test
    public void testGetValues() {
        List<float[]> values = gesture.getValues();
        assertEquals(3, values.size());
        assertArrayEquals(new float[]{1.0f, 2.0f, 3.0f}, values.get(0), 0.001f);
    }

    @Test
    public void testSetValues() {
        List<float[]> newValues = new ArrayList<>();
        newValues.add(new float[]{10.0f, 20.0f, 30.0f});
        gesture.setValues(newValues);
        assertEquals(1, gesture.length());
        assertEquals(10.0f, gesture.getValue(0, 0), 0.001f);
    }

    @Test
    public void testSerializable() {
        // Gesture implements Serializable
        assertTrue(gesture instanceof java.io.Serializable);
    }

    @Test
    public void testEmptyGesture() {
        List<float[]> empty = new ArrayList<>();
        Gesture emptyGesture = new Gesture(empty, "empty");
        assertEquals(0, emptyGesture.length());
        assertEquals("empty", emptyGesture.getLabel());
    }
}
