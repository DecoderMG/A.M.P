package com.dmgproductions.amp.service;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps Google Play Services Activity Recognition API to detect
 * user activities (still, walking, running) with high-level system support.
 * Falls back gracefully if Play Services is unavailable.
 */
public class GoogleActivityRecognitionSource {

    private static final String TAG = "GoogleActivityRecog";
    private static final String ACTION_ACTIVITY_TRANSITION =
            "com.dmgproductions.amp.ACTION_ACTIVITY_TRANSITION";
    private static final String ACTION_ACTIVITY_UPDATE =
            "com.dmgproductions.amp.ACTION_ACTIVITY_UPDATE";
    private static final long DETECTION_INTERVAL_MS = 1000;

    public interface GoogleActivityListener {
        void onActivityDetected(ActivityRecognitionManager.UserActivity activity, int confidence);
    }

    private final Context context;
    private ActivityRecognitionClient activityRecognitionClient;
    private PendingIntent transitionPendingIntent;
    private PendingIntent updatesPendingIntent;
    private GoogleActivityListener listener;
    private boolean isRegistered = false;

    private final MutableLiveData<ActivityRecognitionManager.UserActivity> detectedActivity =
            new MutableLiveData<>(ActivityRecognitionManager.UserActivity.IDLE);

    private final BroadcastReceiver transitionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ActivityTransitionResult.hasResult(intent)) {
                ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
                if (result != null) {
                    for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                        handleTransitionEvent(event);
                    }
                }
            }
        }
    };

    private final BroadcastReceiver updatesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ActivityRecognitionResult.hasResult(intent)) {
                ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
                if (result != null) {
                    DetectedActivity mostProbable = result.getMostProbableActivity();
                    handleDetectedActivity(mostProbable.getType(), mostProbable.getConfidence());
                }
            }
        }
    };

    public GoogleActivityRecognitionSource(Context context) {
        this.context = context.getApplicationContext();
    }

    public void start() {
        try {
            activityRecognitionClient = ActivityRecognition.getClient(context);
            registerTransitions();
            requestActivityUpdates();
        } catch (Exception e) {
            Log.w(TAG, "Google Play Services Activity Recognition not available", e);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void registerTransitions() {
        List<ActivityTransition> transitions = new ArrayList<>();

        // Watch for ENTER transitions for all activities we care about
        int[] activityTypes = {
                DetectedActivity.STILL,
                DetectedActivity.WALKING,
                DetectedActivity.RUNNING,
                DetectedActivity.ON_BICYCLE
        };

        for (int activityType : activityTypes) {
            transitions.add(new ActivityTransition.Builder()
                    .setActivityType(activityType)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build());
            transitions.add(new ActivityTransition.Builder()
                    .setActivityType(activityType)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build());
        }

        ActivityTransitionRequest request = new ActivityTransitionRequest(transitions);

        Intent intent = new Intent(ACTION_ACTIVITY_TRANSITION);
        intent.setPackage(context.getPackageName());
        transitionPendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        activityRecognitionClient.requestActivityTransitionUpdates(request, transitionPendingIntent)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Activity Transition updates registered");
                    registerReceiver();
                })
                .addOnFailureListener(e ->
                        Log.w(TAG, "Failed to register Activity Transitions", e));
    }

    @SuppressWarnings("MissingPermission")
    private void requestActivityUpdates() {
        Intent intent = new Intent(ACTION_ACTIVITY_UPDATE);
        intent.setPackage(context.getPackageName());
        updatesPendingIntent = PendingIntent.getBroadcast(
                context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        activityRecognitionClient.requestActivityUpdates(DETECTION_INTERVAL_MS, updatesPendingIntent)
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "Activity updates registered"))
                .addOnFailureListener(e ->
                        Log.w(TAG, "Failed to register Activity Updates", e));
    }

    private void registerReceiver() {
        if (!isRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_ACTIVITY_TRANSITION);
            filter.addAction(ACTION_ACTIVITY_UPDATE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(transitionReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
                context.registerReceiver(updatesReceiver, new IntentFilter(ACTION_ACTIVITY_UPDATE),
                        Context.RECEIVER_NOT_EXPORTED);
            } else {
                context.registerReceiver(transitionReceiver, filter);
                context.registerReceiver(updatesReceiver, new IntentFilter(ACTION_ACTIVITY_UPDATE));
            }
            isRegistered = true;
        }
    }

    public void stop() {
        if (activityRecognitionClient != null) {
            if (transitionPendingIntent != null) {
                activityRecognitionClient.removeActivityTransitionUpdates(transitionPendingIntent);
            }
            if (updatesPendingIntent != null) {
                activityRecognitionClient.removeActivityUpdates(updatesPendingIntent);
            }
        }
        if (isRegistered) {
            try {
                context.unregisterReceiver(transitionReceiver);
                context.unregisterReceiver(updatesReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver not registered
            }
            isRegistered = false;
        }
    }

    public void setListener(GoogleActivityListener listener) {
        this.listener = listener;
    }

    public MutableLiveData<ActivityRecognitionManager.UserActivity> getDetectedActivity() {
        return detectedActivity;
    }

    private void handleTransitionEvent(ActivityTransitionEvent event) {
        if (event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
            ActivityRecognitionManager.UserActivity mapped = mapGoogleActivity(event.getActivityType());
            detectedActivity.postValue(mapped);
            if (listener != null) {
                listener.onActivityDetected(mapped, 100);
            }
        }
    }

    private void handleDetectedActivity(int activityType, int confidence) {
        if (confidence < 50) return; // Ignore low-confidence detections
        ActivityRecognitionManager.UserActivity mapped = mapGoogleActivity(activityType);
        detectedActivity.postValue(mapped);
        if (listener != null) {
            listener.onActivityDetected(mapped, confidence);
        }
    }

    static ActivityRecognitionManager.UserActivity mapGoogleActivity(int detectedActivityType) {
        switch (detectedActivityType) {
            case DetectedActivity.RUNNING:
            case DetectedActivity.ON_BICYCLE:
                return ActivityRecognitionManager.UserActivity.RUNNING;
            case DetectedActivity.WALKING:
            case DetectedActivity.ON_FOOT:
                return ActivityRecognitionManager.UserActivity.WALKING;
            case DetectedActivity.STILL:
            case DetectedActivity.TILTING:
                return ActivityRecognitionManager.UserActivity.IDLE;
            default:
                return ActivityRecognitionManager.UserActivity.UNKNOWN;
        }
    }
}
