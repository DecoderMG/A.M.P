package com.dmgproductions.amp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.dmgproductions.amp.service.MusicPlaybackService;

public class PlaybackViewModel extends ViewModel {

    private final MutableLiveData<MusicPlaybackService.PlaybackState> playbackState =
            new MutableLiveData<>(MusicPlaybackService.PlaybackState.STOPPED);
    private final MutableLiveData<MusicPlaybackService.SongInfo> currentSong =
            new MutableLiveData<>();
    private final MutableLiveData<Integer> playbackPosition = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> duration = new MutableLiveData<>(0);
    private final MutableLiveData<String> currentActivity = new MutableLiveData<>("idle");
    private final MutableLiveData<Boolean> serviceBound = new MutableLiveData<>(false);

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
}
