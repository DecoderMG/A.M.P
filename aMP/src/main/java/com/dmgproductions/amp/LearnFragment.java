package com.dmgproductions.amp;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.dmgproductions.amp.gestures.IGestureRecognitionListener;
import com.dmgproductions.amp.gestures.IGestureRecognitionService;
import com.dmgproductions.amp.classifier.Distribution;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;


public class LearnFragment extends Fragment
{
	IGestureRecognitionService recognitionService;
	Button learnWalkingButton, learnRunningButton, deleteActivitiesButton;
	TextToSpeech activityLearningTTS;
	
	private boolean learningWalking = false, learningRunning = false, appClosed = false, learning = false;

	private final ServiceConnection serviceConnection = new ServiceConnection()
	{
		
		@Override
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			recognitionService = IGestureRecognitionService.Stub
					.asInterface(service);
			try 
			{
				recognitionService.registerListener(IGestureRecognitionListener.Stub.asInterface(gestureListenerStub));
			} catch (RemoteException e1) {
				e1.printStackTrace();
			}
		}
		
		@Override
		public void onServiceDisconnected(ComponentName className) 
		{
			try 
			{
				recognitionService.stopClassificationMode();
			} catch (RemoteException e) 
			{
				e.printStackTrace();
			}
			recognitionService = null;
		}
	};
	
	IBinder gestureListenerStub = new IGestureRecognitionListener.Stub() 
	{

		@Override
		public void onGestureRecognized(Distribution distribution)
				throws RemoteException 
				{
			// Do nothing
			
		}

		@Override
		public void onGestureLearned(String gestureName) throws RemoteException 
		{
			if(learningWalking && learning == true)
			{
				learnRunningButton.setEnabled(true);
				learningWalking = false;
				activityLearningTTS.speak("Learned walking", TextToSpeech.QUEUE_ADD, null);
			}
			else if(learningRunning && learning == true)
			{
				learnWalkingButton.setEnabled(true);
				learningRunning = false;
				activityLearningTTS.speak("Learned running", TextToSpeech.QUEUE_ADD, null);
			}
			
		}

		@Override
		public void onTrainingSetDeleted(String trainingSet)
				throws RemoteException 
				{
			// Do nothing
			
		}
		
	};
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.learning_activity_layout, container, false);
		learnWalkingButton = (Button)rootView.findViewById(R.id.learnWalkingButton);
		learnRunningButton = (Button)rootView.findViewById(R.id.learnRunningButton);
		deleteActivitiesButton = (Button)rootView.findViewById(R.id.cleanActivities);
		
		deleteActivitiesButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity().getApplicationContext());
				builder.setMessage(
						"You really want to delete the training set?")
						.setCancelable(true)
						.setPositiveButton("Yes",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										if (recognitionService != null) {
											try {
												recognitionService
														.deleteTrainingSet("amp");
											} catch (RemoteException e) {
												// TODO Auto-generated catch
												// block
												e.printStackTrace();
											}
										}
									}
								})
						.setNegativeButton("No",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										dialog.cancel();
									}
								});
				builder.create().show();
			}
		});
		
		final LearnActivityTime timer = new LearnActivityTime(10000, 1000);
		
		activityLearningTTS = new TextToSpeech(getActivity().getApplicationContext(), new TextToSpeech.OnInitListener()
		{
			
			@Override
			public void onInit(int status) 
			{
				if(status == TextToSpeech.SUCCESS)
				{
					activityLearningTTS.setLanguage(Locale.US);
				}
			}
		});
		learnWalkingButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0) 
			{
				try 
				{
					
					if(learningRunning == false)
					{
						learningWalking = true;
						learnRunningButton.setEnabled(false);
						learning = false;
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						recognitionService.startLearnMode("amp", "walking");
						timer.start();
					}
				} catch (RemoteException e) 
				{
					e.printStackTrace();
				}
			}
			
		});
		learnRunningButton.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View arg0) 
			{
				try 
				{
					if(learningWalking == false)
					{
						learningRunning = true;
						learnWalkingButton.setEnabled(false);
						learning = false;
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						recognitionService.startLearnMode("amp", "running");
						timer.start();
					}
				} catch (RemoteException e) 
				{	
					e.printStackTrace();
				}
			}
			
		});
		return rootView;
	}
	
	@Override
	public void onPause() {
		try {
			recognitionService
					.unregisterListener(IGestureRecognitionListener.Stub
							.asInterface(gestureListenerStub));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		recognitionService = null;
		getActivity().getApplicationContext().unbindService(serviceConnection);
		
		//activityLearningTTS.shutdown();
		appClosed = true;
		super.onPause();
	}

	@Override
	public void onResume() {
		Intent bindIntent = new Intent(
				"com.dmgproductions.amp.gestures.GESTURE_RECOGNIZER");
		getActivity().getApplicationContext().bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
		appClosed = false;
		super.onResume();
	}
	
	
	public class LearnActivityTime extends CountDownTimer {
		public LearnActivityTime(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
		}

		@Override
		public void onFinish() 
		{
			try {
				recognitionService.callRecognition();
				recognitionService.stopLearnMode();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			activityLearningTTS.speak("Learning Sequence Complete", TextToSpeech.QUEUE_ADD, null);
		}

		@SuppressLint({ "NewApi", "DefaultLocale" })
		@Override
		public void onTick(long millisUntilFinished) {
			long millis = millisUntilFinished;
			String hms = String.format("%02d",
					TimeUnit.MILLISECONDS.toSeconds(millis));
			if(millis < 4000)
			{
				learning = true;
				
			}
			//tts.speak(hms, TextToSpeech.QUEUE_ADD, null);
		}
	}

}
