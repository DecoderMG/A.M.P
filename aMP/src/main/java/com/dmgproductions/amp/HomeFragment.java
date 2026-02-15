package com.dmgproductions.amp;

import java.util.Locale;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff.Mode;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import androidx.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dmgproductions.amp.classifier.Distribution;
import com.dmgproductions.amp.gestures.IGestureRecognitionListener;
import com.dmgproductions.amp.gestures.IGestureRecognitionService;
import com.dmgproductions.amp.service.ActivityBridge;
import com.dmgproductions.amp.service.ActivityRecognitionManager;
import com.dmgproductions.amp.service.MusicPlaybackService;
import com.dmgproductions.amp.service.TempoMatcher;
import com.dmgproductions.amp.viewmodel.PlaybackViewModel;
import com.dmgproductions.amp.visualizer.VisualizerView;
import com.dmgproductions.amp.visualizer.renderer.CircleBarRenderer;
import com.triggertrap.seekarc.SeekArc;
import com.triggertrap.seekarc.SeekArc.OnSeekArcChangeListener;


public class HomeFragment extends Fragment implements AnimationListener
{
	private VisualizerView mVisualizerView;
	private View albumArtworkHolder;
	private SeekArc musicSeekBar;
	
	private IGestureRecognitionService recognitionService;;
	
	private Animation flipZoomOut;
	private Animation flipZoomIn;
	private Animation fade;
	
	private TextToSpeech tts;
	
	private String lastActivity = "";

	private int sameActivity = 0;

	private boolean seekBarMoving = false, visualizerCheck = false,
			modulation = true, playButtonShowing = true, artistDisplayed = false;
	private Button playPauseButton;

	private TextView songNameText, artistNameText, albumNameText, activityText;

	// Phase 3: Activity recognition + tempo matching
	private ActivityBridge activityBridge;
	private PlaybackViewModel playbackViewModel;

	// Phase 4: Modern playback service binding
	private MusicPlaybackService musicService;
	private boolean musicServiceBound = false;
	
	private double finalTime = 0.0;
	private double startTime = 0.0;
	
	private Handler myHandler = new Handler();
    
	public HomeFragment()
    {}
     
	private final ServiceConnection serviceConnection = new ServiceConnection()
	{

		@Override
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			recognitionService = IGestureRecognitionService.Stub
					.asInterface(service);
			try
			{
				recognitionService.startClassificationMode("amp");
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

	// Phase 4: Music playback service connection
	private final ServiceConnection musicServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			MusicPlaybackService.LocalBinder binder = (MusicPlaybackService.LocalBinder) service;
			musicService = binder.getService();
			musicServiceBound = true;
			playbackViewModel.setServiceBound(true);

			musicService.loadMusicLibrary();

			// Observe playback state
			musicService.getPlaybackState().observe(getViewLifecycleOwner(), state -> {
				playbackViewModel.setPlaybackState(state);
				if (state == MusicPlaybackService.PlaybackState.PLAYING) {
					playButtonShowing = false;
					playPauseButton.setBackgroundResource(R.drawable.stopbutton);
				} else {
					playButtonShowing = true;
					playPauseButton.setBackgroundResource(R.drawable.playbutton);
				}
			});

			// Observe current song
			musicService.getCurrentSong().observe(getViewLifecycleOwner(), song -> {
				if (song != null) {
					playbackViewModel.setCurrentSong(song);
					songNameText.setText(song.title);
					artistNameText.setText(song.artist);
					albumNameText.setText(song.album);
				}
			});

			// Observe playback speed changes
			musicService.getCurrentPlaybackSpeed().observe(getViewLifecycleOwner(), speed -> {
				playbackViewModel.setPlaybackSpeed(speed);
			});

			// Start seek bar update loop
			startSeekBarUpdater();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			musicService = null;
			musicServiceBound = false;
			playbackViewModel.setServiceBound(false);
		}
	};
	
	IBinder gestureListenerStub = new IGestureRecognitionListener.Stub() 
	{

		@Override
		public void onGestureRecognized(final Distribution distribution)
				throws RemoteException 
				{
			getActivity().runOnUiThread(new Runnable() 
			{
				@Override
				public void run() 
				{
					/*Toast.makeText(
							GestureTrainer.this,
							String.format("%s: %f",
									distribution.getBestMatch(),
									distribution.getBestDistance()),
							Toast.LENGTH_LONG).show(); */
					
					tts.speak(
							String.format(
									"Recognized as %s at a %.2f Best Distance",
									distribution.getBestMatch().toString(),
									distribution.getBestDistance()),
							TextToSpeech.QUEUE_FLUSH, null); 
					System.err.println(String.format("%s: %f",
							distribution.getBestMatch(),
							distribution.getBestDistance()));
					String currentActivity = distribution.getBestMatch().toString();
					if (sameActivity < 1) 
					{
						if (sameActivity <= 0) 
						{
							lastActivity = currentActivity;
							sameActivity = sameActivity + 1;
						} 
						else if (lastActivity.equals(currentActivity)) 
						{
							sameActivity = sameActivity + 1;
						} 
						else 
						{
							sameActivity = 0;
							tts.speak("Activity Changed One", TextToSpeech.QUEUE_FLUSH, null);
						}
					} 
					else if (sameActivity >= 1) 
					{
						if (lastActivity.equals(currentActivity)) 
						{
							lastActivity = currentActivity;
							sameActivity = sameActivity + 1;
							Toast.makeText(getActivity().getBaseContext(),
									"current activity: " + lastActivity,
									Toast.LENGTH_SHORT).show();
							if(sameActivity < 3)
							{
								tts.speak("Current activity is " + lastActivity, TextToSpeech.QUEUE_FLUSH, null);
								activityText.setText("Currently: "+lastActivity);
							}
							/*if(firstRun)
							{
								if(lastActivity.equals("walking"))
								{
									walkingMP.start();
									//startService(new Intent(getApplicationContext(), WalkingMediaActivity.class));
								}
								if(lastActivity.equals("running"))
								{
									runningMP.start();
									//startService(new Intent(getApplicationContext(), RunningMediaActivity.class));
								}
							}
							else if(!firstRun && lastActivity.equals("walking"))
							{
								if(runningMP.isPlaying())
								{
									runningMP.pause();
									walkingMP.start();
								}
								else if(walkingMP.isPlaying())
								{}
								//stopService(new Intent(getApplicationContext(), RunningMediaActivity.class));
								//startService(new Intent(getApplicationContext(), WalkingMediaActivity.class));
							}
							else if(!firstRun && lastActivity.equals("running"))
							{
								if(walkingMP.isPlaying())
								{
									walkingMP.pause();
									runningMP.start();
								}
								else if(runningMP.isPlaying())
								{}
								//stopService(new Intent(getApplicationContext(), WalkingMediaActivity.class));
								//startService(new Intent(getApplicationContext(), RunningMediaActivity.class));
							}*/
								
						} else 
						{
							sameActivity = 0;
							tts.speak("Activity Changed Two", TextToSpeech.QUEUE_FLUSH, null);
						}
						//firstRun = false;
					}
					//final DetectActivityTimer dat = new DetectActivityTimer(2000, 1000);
					//dat.start();
				}
			});
			// Do nothing
			
		}

		@Override
		public void onGestureLearned(String gestureName) throws RemoteException 
		{
			
		}

		@Override
		public void onTrainingSetDeleted(String trainingSet)
				throws RemoteException 
				{
			// Do nothing
		}
		
	};
	
	
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) 
    {   
    	View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        
        songNameText = (TextView)rootView.findViewById(R.id.Song_Name_Label);
        artistNameText = (TextView)rootView.findViewById(R.id.Artist_Name_Label);
        albumNameText = (TextView)rootView.findViewById(R.id.Album_Name_Label);
        activityText = (TextView)rootView.findViewById(R.id.activityText);
        
        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "fonts/roboto_thin.ttf");
        songNameText.setTypeface(font, Typeface.BOLD_ITALIC);
        artistNameText.setTypeface(font, Typeface.ITALIC);
        albumNameText.setTypeface(font, Typeface.BOLD);
        
        tts = new TextToSpeech(getActivity().getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
            }
        });
        
        flipZoomOut = AnimationUtils.loadAnimation(getActivity(), R.anim.zoom_out);
        flipZoomIn = AnimationUtils.loadAnimation(getActivity(), R.anim.zoom_in);
        fade = AnimationUtils.loadAnimation(getActivity(), R.anim.fade);
        
        flipZoomOut.setAnimationListener(this);
        flipZoomIn.setAnimationListener(this);
        
        albumArtworkHolder = (View)rootView.findViewById(R.id.albumHolder);
        
      
        
        
     // We need to link the visualizer view to the media player so that
        // it displays something
        mVisualizerView = (VisualizerView)rootView.findViewById(R.id.visualizerView);
        //mVisualizerView.link(mWalkingPlayer);
        playPauseButton = (Button)rootView.findViewById(R.id.play_pause_button);
        playPauseButton.setOnClickListener(v -> {
            if (musicServiceBound && musicService != null) {
                // Phase 4: Use MusicPlaybackService (ExoPlayer)
                if (musicService.isPlaying()) {
                    musicService.pause();
                } else {
                    musicService.play();
                }
                playPauseButton.clearAnimation();
                playPauseButton.setAnimation(flipZoomIn);
                playPauseButton.startAnimation(flipZoomIn);
            }
        });

        Button nextButton = (Button)rootView.findViewById(R.id.next_button);
        nextButton.setOnClickListener(v -> {
            if (musicServiceBound && musicService != null) {
                musicService.playNext();
            }
        });
        
        // Phase 3: Initialize activity bridge and ViewModel
        playbackViewModel = new ViewModelProvider(requireActivity()).get(PlaybackViewModel.class);
        activityBridge = new ActivityBridge(requireContext());
        activityBridge.setListener((activity, cadenceBPM, confidence) -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    playbackViewModel.updateActivityState(activity, cadenceBPM, confidence);
                    String activityLabel = activity.name().toLowerCase();
                    String bpmStr = cadenceBPM > 0 ? String.format(Locale.US, " (%.0f BPM)", cadenceBPM) : "";
                    activityText.setText("Currently: " + activityLabel + bpmStr);

                    // Phase 4: Send cadence BPM to playback service for tempo matching
                    if (musicServiceBound && musicService != null && cadenceBPM > 0) {
                        float targetMusicBPM = TempoMatcher.matchMusicTempo(cadenceBPM);
                        musicService.setTargetBPM(targetMusicBPM);
                    }
                });
            }
        });

        // Observe tempo range changes for queue rebuilding
        playbackViewModel.getTempoRange().observe(getViewLifecycleOwner(), tempoRange -> {
            if (musicServiceBound && musicService != null && tempoRange != null) {
                Float bpm = playbackViewModel.getCadenceBPM().getValue();
                if (bpm != null && bpm > 0) {
                    musicService.buildTempoMatchedQueue(
                            TempoMatcher.matchMusicTempo(bpm), 15f);
                }
            }
        });

        musicSeekBar = (SeekArc)rootView.findViewById(R.id.seekArc);
        musicSeekBar.setOnSeekArcChangeListener(new OnSeekArcChangeListener() 
        {
			
			@Override
			public void onStopTrackingTouch(SeekArc seekArc)
			{
				seekBarMoving = false;
				if(musicServiceBound && musicService != null && musicService.isPlaying())
				{
					musicService.seekTo((int)startTime);
				}

			}
			
			@Override
			public void onStartTrackingTouch(SeekArc seekArc) 
			{
				seekBarMoving = true;
			}
			
			@Override
			public void onProgressChanged(SeekArc seekArc, int progress,
					boolean fromUser)
			{
				if(musicServiceBound && musicService != null && musicService.isPlaying())
				{
					startTime = progress;
				}
			}
		});
        return rootView;
    }
    
    @Override
    public void onResume()
    {
    	super.onResume();
    	
    	Intent bindIntent = new Intent(getActivity(), com.dmgproductions.amp.gestures.GestureRecognitionService.class);
		getActivity().bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);

    	// Phase 4: Bind to MusicPlaybackService
    	Intent musicIntent = new Intent(getActivity(), MusicPlaybackService.class);
    	getActivity().startService(musicIntent);
    	getActivity().bindService(musicIntent, musicServiceConnection, Context.BIND_AUTO_CREATE);

    	// Phase 3: Start activity detection
    	if (activityBridge != null) {
    		activityBridge.start();
    	}
    	
    	SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
    	
    	if(sharedPref.getBoolean("pref_visualizer_modulation_toggle", false) == true)
    	{
    		modulation = true;
    	}
    	else
    	{
    		modulation = false;
    	}
    	if(sharedPref.getBoolean("pref_visualizer_toggle", false) == true)
    	{
    		visualizerCheck = true;
    		// Visualizer linking deferred until ExoPlayer audio session is available
    		addCircleBarRenderer();
    	}
    	if(sharedPref.getBoolean("pref_album_artwork_setting", false) == true)
    	{
    		albumArtworkHolder.setBackgroundColor(Color.BLUE);
    	}
    	else
    	{
    		albumArtworkHolder.setBackgroundColor(Color.parseColor("#00222222"));
    	}
    }
    
    @Override
	public void onPause()
    {
      cleanUp();

      // Phase 3: Stop activity detection
      if (activityBridge != null) {
          activityBridge.stop();
      }

      try {
			recognitionService
					.unregisterListener(IGestureRecognitionListener.Stub
							.asInterface(gestureListenerStub));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		recognitionService = null;
		getActivity().unbindService(serviceConnection);

      // Phase 4: Unbind music service (don't stop it — keeps playing in background)
      if (musicServiceBound) {
          getActivity().unbindService(musicServiceConnection);
          musicServiceBound = false;
      }

      if(visualizerCheck)
    	  mVisualizerView.release();
      super.onPause();
    }

    @Override
	public void onDestroy()
    {
      cleanUp();
      super.onDestroy();
    }
    
    
    //Initialization of seek bar update loop
    private void startSeekBarUpdater()
    {
      musicSeekBar.setScrollbarFadingEnabled(true);
      myHandler.removeCallbacks(UpdateSongTime);
      myHandler.postDelayed(UpdateSongTime, 100);
    }

    //Recursive runnable to update seekbar with song progression
	private Runnable UpdateSongTime = new Runnable()
	{
	    public void run()
	    {
		    if(musicServiceBound && musicService != null && musicService.isPlaying())
		    {
		    	if(!seekBarMoving)
		    	{
		    		startTime = musicService.getCurrentPosition();
		    		musicSeekBar.setProgress((int)startTime);
		    	}
		    	// Update seek bar max from service duration
		    	int duration = musicService.getDuration();
		    	if(duration > 0)
		    	{
		    		musicSeekBar.setMax(duration);
		    	}
		    }
		    myHandler.postDelayed(this, 100);
	    }
	};
    private void cleanUp()
    {
      myHandler.removeCallbacks(UpdateSongTime);

      if(visualizerCheck && mVisualizerView != null)
      {
          mVisualizerView.clearAnimation();
          mVisualizerView.clearRenderers();
          mVisualizerView.release();
      }
    }
    
    private void addCircleBarRenderer()
    {
      Paint paint = new Paint();
      paint.setStrokeWidth(10f);
      paint.setAntiAlias(true);
      paint.setXfermode(new PorterDuffXfermode(Mode.LIGHTEN));
      paint.setColor(Color.argb(255, 0, 221, 255));
      CircleBarRenderer circleBarRenderer = new CircleBarRenderer(paint, 10, false, modulation);
      mVisualizerView.addRenderer(circleBarRenderer);
    }

	@Override
	public void onAnimationEnd(Animation currentAnimation) 
	{
		if(currentAnimation == flipZoomIn)
		{
			if(playButtonShowing)
			{
				playPauseButton.setBackgroundResource(R.drawable.stopbutton);
				playButtonShowing = false;
			}
			else
			{
				playPauseButton.setBackgroundResource(R.drawable.playbutton);
				playButtonShowing = true;
			}
			playPauseButton.clearAnimation();
			playPauseButton.setAnimation(flipZoomOut);
			playPauseButton.startAnimation(flipZoomOut);
		}
		if(currentAnimation == fade)
		{
			if(musicServiceBound && musicService != null && musicService.isPlaying())
			{
				MusicPlaybackService.SongInfo song = musicService.getCurrentSong().getValue();
				if(song != null)
				{
					if(artistDisplayed)
					{
						artistNameText.setText(song.album);
						artistDisplayed = false;
					}
					else
					{
						artistNameText.setText(song.artist);
						artistDisplayed = true;
					}
				}
			}
			artistNameText.clearAnimation();
			artistNameText.setAnimation(fade);
			artistNameText.startAnimation(fade);
		}
		
	}

	@Override
	public void onAnimationRepeat(Animation arg0) 
	{	
		// TODO Auto-generated method stub
	}

	@Override
	public void onAnimationStart(Animation arg0) 
	{
		// TODO Auto-generated method stub
	}
}