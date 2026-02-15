package com.dmgproductions.amp.service;

/**
 * Maps detected user activity and step cadence (BPM) to target music tempo ranges.
 *
 * This is the core of A.M.P's concept: matching the music's tempo to the user's
 * physical activity so that beats align with footsteps/motion.
 *
 * Typical cadence ranges:
 *   - Walking: 100-130 steps/min
 *   - Jogging: 140-170 steps/min
 *   - Running: 170-200 steps/min
 *
 * Music can match at 1:1 (every beat = one step) or 2:1 (every other beat = one step).
 */
public class TempoMatcher {

    public static class TempoRange {
        public final float minBPM;
        public final float maxBPM;
        public final float targetBPM;

        public TempoRange(float minBPM, float maxBPM, float targetBPM) {
            this.minBPM = minBPM;
            this.maxBPM = maxBPM;
            this.targetBPM = targetBPM;
        }

        public boolean contains(float bpm) {
            return bpm >= minBPM && bpm <= maxBPM;
        }
    }

    // Default tempo ranges for each activity type
    private static final TempoRange IDLE_RANGE = new TempoRange(60, 100, 80);
    private static final TempoRange WALKING_RANGE = new TempoRange(95, 135, 115);
    private static final TempoRange RUNNING_RANGE = new TempoRange(130, 200, 160);

    /**
     * Get the target music tempo range for a given user activity.
     */
    public static TempoRange getTempoRangeForActivity(ActivityRecognitionManager.UserActivity activity) {
        switch (activity) {
            case WALKING:
                return WALKING_RANGE;
            case RUNNING:
                return RUNNING_RANGE;
            case IDLE:
            default:
                return IDLE_RANGE;
        }
    }

    /**
     * Given the user's detected cadence BPM, determine the ideal music tempo.
     * Music tempo can be 1:1 with step cadence, or we can use half-time / double-time.
     *
     * @param cadenceBPM The user's step cadence in steps per minute
     * @return The ideal music BPM to match the cadence
     */
    public static float matchMusicTempo(float cadenceBPM) {
        if (cadenceBPM <= 0) return 0;

        // Direct match: use cadence as-is if it falls in typical music tempo range
        if (cadenceBPM >= 80 && cadenceBPM <= 200) {
            return cadenceBPM;
        }

        // Double-time: if cadence is very slow, double it to get into music range
        if (cadenceBPM >= 40 && cadenceBPM < 80) {
            return cadenceBPM * 2;
        }

        // Half-time: if cadence is very fast (sprinting), halve it
        if (cadenceBPM > 200 && cadenceBPM <= 400) {
            return cadenceBPM / 2;
        }

        return cadenceBPM;
    }

    /**
     * Score how well a song's BPM matches the target cadence.
     * Returns a value between 0.0 (no match) and 1.0 (perfect match).
     *
     * Considers both direct tempo match and harmonic matches (half/double time).
     *
     * @param songBPM The song's BPM
     * @param targetBPM The target cadence BPM
     * @param tolerance How many BPM of deviation is acceptable (e.g., 10)
     * @return Match score from 0.0 to 1.0
     */
    public static float scoreSongMatch(float songBPM, float targetBPM, float tolerance) {
        if (targetBPM <= 0 || songBPM <= 0) return 0;

        // Check direct match
        float directDiff = Math.abs(songBPM - targetBPM);
        float directScore = Math.max(0, 1.0f - (directDiff / tolerance));

        // Check half-time match (song is half the cadence)
        float halfTimeDiff = Math.abs(songBPM - targetBPM / 2);
        float halfTimeScore = Math.max(0, 1.0f - (halfTimeDiff / tolerance)) * 0.7f;

        // Check double-time match (song is double the cadence)
        float doubleTimeDiff = Math.abs(songBPM - targetBPM * 2);
        float doubleTimeScore = Math.max(0, 1.0f - (doubleTimeDiff / tolerance)) * 0.7f;

        return Math.max(directScore, Math.max(halfTimeScore, doubleTimeScore));
    }

    /**
     * Calculate the playback speed adjustment needed to match a song's tempo
     * to the target cadence. Returns a speed multiplier (e.g., 1.1 = 10% faster).
     *
     * Clamps to reasonable adjustment range to avoid audio artifacts.
     *
     * @param songBPM The song's original BPM
     * @param targetBPM The desired BPM to match
     * @param maxAdjustment Maximum speed adjustment (e.g., 0.15 for ±15%)
     * @return Playback speed multiplier (1.0 = no change)
     */
    public static float calculatePlaybackSpeed(float songBPM, float targetBPM, float maxAdjustment) {
        if (songBPM <= 0 || targetBPM <= 0) return 1.0f;

        float ratio = targetBPM / songBPM;

        // Check if half-time or double-time is a closer match
        float halfRatio = (targetBPM / 2) / songBPM;
        float doubleRatio = (targetBPM * 2) / songBPM;

        // Pick whichever ratio is closest to 1.0
        float bestRatio = ratio;
        if (Math.abs(halfRatio - 1.0f) < Math.abs(bestRatio - 1.0f)) {
            bestRatio = halfRatio;
        }
        if (Math.abs(doubleRatio - 1.0f) < Math.abs(bestRatio - 1.0f)) {
            bestRatio = doubleRatio;
        }

        // Clamp to maximum adjustment range
        float minSpeed = 1.0f - maxAdjustment;
        float maxSpeed = 1.0f + maxAdjustment;
        return Math.max(minSpeed, Math.min(maxSpeed, bestRatio));
    }
}
