package com.dmgproductions.amp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.dmgproductions.amp.service.ActivityRecognitionManager;
import com.dmgproductions.amp.service.MusicPlaybackService;
import com.dmgproductions.amp.service.TempoMatcher;

public class PlaybackViewModel extends ViewModel {

    private final MutableLiveData<MusicPlaybackService.PlaybackState> playbackState =
            new MutableLiveData<>(MusicPlaybackService.PlaybackState.STOPPED);
    private final MutableLiveData<MusicPlaybackService.SongInfo> currentSong =
            new MutableLiveData<>();
    private final MutableLiveData<Integer> playbackPosition = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> duration = new MutableLiveData<>(0);
    private final MutableLiveData<String> currentActivity = new MutableLiveData<>("idle");
    private final MutableLiveData<Boolean> serviceBound = new MutableLiveData<>(false);

    // Phase 3: Activity recognition + tempo matching fields
    private final MutableLiveData<ActivityRecognitionManager.UserActivity> detectedActivity =
            new MutableLiveData<>(ActivityRecognitionManager.UserActivity.IDLE);
    private final MutableLiveData<Float> cadenceBPM = new MutableLiveData<>(0f);
    private final MutableLiveData<Float> targetMusicBPM = new MutableLiveData<>(0f);
    private final MutableLiveData<Float> playbackSpeed = new MutableLiveData<>(1.0f);
    private final MutableLiveData<Float> activityConfidence = new MutableLiveData<>(0f);
    private final MutableLiveData<TempoMatcher.TempoRange> tempoRange =
            new MutableLiveData<>(TempoMatcher.getTempoRangeForActivity(
                    ActivityRecognitionManager.UserActivity.IDLE));
    private final MutableLiveData<Integer> stepCount = new MutableLiveData<>(0);

    // --- Existing accessors ---

    public LiveData<MusicPlaybackService.PlaybackState> getPlaybackState() {
        return playbackState;
    }

    public LiveData<MusicPlaybackService.SongInfo> getCurrentSong() {
        return currentSong;
    }

    public LiveData<Integer> getPlaybackPosition() {
        return playbackPosition;
    }

    public LiveData<Integer> getDuration() {
        return duration;
    }

    public LiveData<String> getCurrentActivity() {
        return currentActivity;
    }

    public LiveData<Boolean> getServiceBound() {
        return serviceBound;
    }

    public void setPlaybackState(MusicPlaybackService.PlaybackState state) {
        playbackState.setValue(state);
    }

    public void setCurrentSong(MusicPlaybackService.SongInfo song) {
        currentSong.setValue(song);
    }

    public void setPlaybackPosition(int position) {
        playbackPosition.setValue(position);
    }

    public void setDuration(int dur) {
        duration.setValue(dur);
    }

    public void setCurrentActivity(String activity) {
        currentActivity.setValue(activity);
    }

    public void setServiceBound(boolean bound) {
        serviceBound.setValue(bound);
    }

    // --- Phase 3: Activity + Tempo accessors ---

    public LiveData<ActivityRecognitionManager.UserActivity> getDetectedActivity() {
        return detectedActivity;
    }

    public void setDetectedActivity(ActivityRecognitionManager.UserActivity activity) {
        detectedActivity.setValue(activity);
        currentActivity.setValue(activity.name().toLowerCase());
    }

    public LiveData<Float> getCadenceBPM() {
        return cadenceBPM;
    }

    public void setCadenceBPM(float bpm) {
        cadenceBPM.setValue(bpm);
        // Auto-compute target music tempo from cadence
        float musicBPM = TempoMatcher.matchMusicTempo(bpm);
        targetMusicBPM.setValue(musicBPM);
    }

    public LiveData<Float> getTargetMusicBPM() {
        return targetMusicBPM;
    }

    public LiveData<Float> getPlaybackSpeed() {
        return playbackSpeed;
    }

    public void setPlaybackSpeed(float speed) {
        playbackSpeed.setValue(speed);
    }

    public LiveData<Float> getActivityConfidence() {
        return activityConfidence;
    }

    public void setActivityConfidence(float confidence) {
        activityConfidence.setValue(confidence);
    }

    public LiveData<TempoMatcher.TempoRange> getTempoRange() {
        return tempoRange;
    }

    public void setTempoRange(TempoMatcher.TempoRange range) {
        tempoRange.setValue(range);
    }

    public LiveData<Integer> getStepCount() {
        return stepCount;
    }

    public void setStepCount(int count) {
        stepCount.setValue(count);
    }

    /**
     * Convenience method: update all activity-related fields at once from ActivityBridge.
     */
    public void updateActivityState(ActivityRecognitionManager.UserActivity activity,
                                     float bpm, float confidence) {
        setDetectedActivity(activity);
        setCadenceBPM(bpm);
        setActivityConfidence(confidence);
        setTempoRange(TempoMatcher.getTempoRangeForActivity(activity));
    }
}
