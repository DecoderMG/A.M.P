package com.dmgproductions.amp.classifier;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DistributionTest {

    private Distribution distribution;

    @Before
    public void setUp() {
        distribution = new Distribution();
    }

    @Test
    public void testEmptyDistribution() {
        assertNull("Best match should be null for empty distribution", distribution.getBestMatch());
        assertEquals(0, distribution.size());
    }

    @Test
    public void testSingleEntry() {
        distribution.addEntry("walking", 1.5);

        assertEquals("walking", distribution.getBestMatch());
        assertEquals(1.5, distribution.getBestDistance(), 0.001);
        assertEquals(1, distribution.size());
    }

    @Test
    public void testBestMatchIsMinDistance() {
        distribution.addEntry("walking", 3.0);
        distribution.addEntry("running", 1.0);
        distribution.addEntry("standing", 5.0);

        assertEquals("running", distribution.getBestMatch());
        assertEquals(1.0, distribution.getBestDistance(), 0.001);
        assertEquals(3, distribution.size());
    }

    @Test
    public void testDuplicateLabelKeepsMinDistance() {
        distribution.addEntry("walking", 3.0);
        distribution.addEntry("walking", 1.0);

        assertEquals("walking", distribution.getBestMatch());
        assertEquals(1.0, distribution.getBestDistance(), 0.001);
        assertEquals(1, distribution.size());
    }

    @Test
    public void testDuplicateLabelDoesNotOverwriteWithHigherDistance() {
        distribution.addEntry("walking", 1.0);
        distribution.addEntry("walking", 5.0);

        assertEquals("walking", distribution.getBestMatch());
        assertEquals(1.0, distribution.getBestDistance(), 0.001);
    }

    @Test
    public void testMultipleLabelsWithSameDistance() {
        distribution.addEntry("walking", 2.0);
        distribution.addEntry("running", 2.0);

        assertNotNull(distribution.getBestMatch());
        assertEquals(2.0, distribution.getBestDistance(), 0.001);
        assertEquals(2, distribution.size());
    }

    @Test
    public void testFirstEntryWinsOnTiedDistance() {
        distribution.addEntry("walking", 2.0);
        distribution.addEntry("running", 2.0);

        // First entry at minimum distance should remain the best match
        assertEquals("walking", distribution.getBestMatch());
    }

    @Test
    public void testNewBestUpdatesCorrectly() {
        distribution.addEntry("standing", 5.0);
        assertEquals("standing", distribution.getBestMatch());

        distribution.addEntry("walking", 3.0);
        assertEquals("walking", distribution.getBestMatch());

        distribution.addEntry("running", 1.0);
        assertEquals("running", distribution.getBestMatch());
        assertEquals(1.0, distribution.getBestDistance(), 0.001);
    }
}
