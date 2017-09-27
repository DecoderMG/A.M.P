package com.dmgproductions.amp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff.Mode;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
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
import com.dmgproductions.amp.utils.TunnelPlayerWorkaround;
import com.dmgproductions.amp.visualizer.VisualizerView;
import com.dmgproductions.amp.visualizer.renderer.CircleBarRenderer;
import com.triggertrap.seekarc.SeekArc;
import com.triggertrap.seekarc.SeekArc.OnSeekArcChangeListener;


public class HomeFragment extends Fragment implements AnimationListener
{
	private MediaPlayer mWalkingPlayer, mRunningPlayer;
	private MediaPlayer mSilentPlayer;  /* to avoid tunnel player issue */
	private String[] songData;
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
	
	private Random r;
	
	private boolean seekBarMoving = false, visualizerCheck = false, 
			mWalkingPlayerCheck = false, modulation = true, 
			playButtonShowing = true, artistDisplayed = false;
	private Button playPauseButton;
	
	private TextView songNameText, artistNameText, albumNameText, activityText;
	
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
        
        mWalkingPlayer = new MediaPlayer();
        
        tts = new TextToSpeech(getActivity().getApplicationContext(), new TextToSpeech.OnInitListener()
		{
			
			@Override
			public void onInit(int status) 
			{
				if(status == TextToSpeech.SUCCESS)
				{
					tts.setLanguage(Locale.US);
				}
			}
		});
        
        //mWalkingPlayer = new MediaPlayer();
        //mWalkingPlayer = MediaPlayer.create(getActivity(), R.raw.faint);
        
        ContentResolver musicResolver = getActivity().getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        
      //Some audio may be explicitly marked as not being music
  		String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

  		String[] projection = 
  		{
  		        MediaStore.Audio.Media._ID,
  		        MediaStore.Audio.Media.ARTIST,
  		        MediaStore.Audio.Media.TITLE,
  		        MediaStore.Audio.Media.ALBUM,
  		        MediaStore.Audio.Media.DATA,
  		        MediaStore.Audio.Media.DISPLAY_NAME,
  		        MediaStore.Audio.Media.DURATION
  		};
  		
  		
        /*
  		Cursor musicCursor = musicResolver.query(musicUri, projection, selection, null, null);

  		final List<String> songs = new ArrayList<String>();
  		    while(musicCursor.moveToNext())
  		    {
  		        songs.add(musicCursor.getString(0) + ":" + musicCursor.getString(1) + ":" +   musicCursor.getString(2) + ":" +   musicCursor.getString(3) + ":" +  musicCursor.getString(4) + ":" +  musicCursor.getString(5));
  		    }
  		  r = new Random();
		    int random = r.nextInt((songs.size()) - 0) + 0;
		    songData = songs.get(random).split(":");
        */
        if(mWalkingPlayerCheck)
        {
        	Toast.makeText(getActivity(), "Player is playing", Toast.LENGTH_SHORT).show();
        }
        else
        {
        	
        	mWalkingPlayer.setOnCompletionListener(new OnCompletionListener()
        	{

				@Override
				public void onCompletion(MediaPlayer arg0) {
					// TODO Auto-generated method stub
					r = new Random();
	      		   // int random = r.nextInt((songs.size()) - 0) + 0;
	      		    //songData = songs.get(random).split(":");
	      		  try {
	        			mWalkingPlayer.setDataSource(songData[4].toString());
	        			mWalkingPlayer.prepare();
	        		} catch (IllegalArgumentException e1) {
	        			// TODO Auto-generated catch block
	        			e1.printStackTrace();
	        		} catch (SecurityException e1) {
	        			// TODO Auto-generated catch block
	        			e1.printStackTrace();
	        		} catch (IllegalStateException e1) {
	        			// TODO Auto-generated catch block
	        			e1.printStackTrace();
	        		} catch (IOException e1) {
	        			// TODO Auto-generated catch block
	        			e1.printStackTrace();
	        		}
					
				}
        		
        	});
        	mWalkingPlayerCheck = true;
        }
        
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
        playPauseButton.setOnClickListener(new OnClickListener() 
        {	
			@Override
			public void onClick(View v) 
			{
				if(mWalkingPlayer.isPlaying())
				{
					playPauseButton.clearAnimation();
					playPauseButton.setAnimation(flipZoomIn);
					playPauseButton.startAnimation(flipZoomIn);
					mWalkingPlayer.pause();
					mWalkingPlayerCheck = false;
				}
				else
				{
					try {
	        			mWalkingPlayer.setDataSource(songData[4].toString());
	        			mWalkingPlayer.prepare();
	        		} catch (IllegalArgumentException e1) {
	        			// TODO Auto-generated catch block
	        			e1.printStackTrace();
	        		} catch (SecurityException e1) {
	        			// TODO Auto-generated catch block
	        			e1.printStackTrace();
	        		} catch (IllegalStateException e1) {
	        			// TODO Auto-generated catch block
	        			e1.printStackTrace();
	        		} catch (IOException e1) {
	        			// TODO Auto-generated catch block
	        			e1.printStackTrace();
	        		}
					
					playPauseButton.clearAnimation();
					playPauseButton.setAnimation(flipZoomIn);
					playPauseButton.startAnimation(flipZoomIn);
					initTunnelPlayerWorkaround();
					init();
					songNameText.setText(songData[2].toString());
					artistNameText.setText(songData[1].toString());
					albumNameText.setText(songData[3].toString());
					artistNameText.clearAnimation();
					artistNameText.setAnimation(fade);
					artistNameText.startAnimation(fade);
					artistDisplayed = true;
					mWalkingPlayerCheck = true;
				}
				
			}
		});
        
        Button nextButton = (Button)rootView.findViewById(R.id.next_button);
        nextButton.setOnClickListener(new OnClickListener()
        {

			@Override
			public void onClick(View arg0) 
			{
				if(mWalkingPlayer.isPlaying())
				{
					mVisualizerView.flash();
					mWalkingPlayer.stop();
					r = new Random();
	      		   // int random = r.nextInt((songs.size()) - 0) + 0;
	      		   // songData = songs.get(random).split(":");
	      		  try {
	      			  
	        			mWalkingPlayer.setDataSource(songData[4].toString());
	        			mWalkingPlayer.prepare();
	        			mWalkingPlayer.start();
	        		} catch (IllegalArgumentException e1) {
	        			// TODO Auto-generated catch block
	        			e1.printStackTrace();
	        		} catch (SecurityException e1) {
	        			// TODO Auto-generated catch block
	        			e1.printStackTrace();
	        		} catch (IllegalStateException e1) {
	        			// TODO Auto-generated catch block
	        			e1.printStackTrace();
	        		} catch (IOException e1) {
	        			// TODO Auto-generated catch block
	        			e1.printStackTrace();
	        		}
					
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
				if(mWalkingPlayer.isPlaying())
				{
					mWalkingPlayer.seekTo((int)startTime);
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
				if(mWalkingPlayer.isPlaying())
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
    	
    	Intent bindIntent = new Intent(
				"com.dmgproductions.amp.gestures.GESTURE_RECOGNIZER");
		getActivity().bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    	
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
    		if(mWalkingPlayer != null)
    			mVisualizerView.link(mWalkingPlayer);
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
      try {
			recognitionService
					.unregisterListener(IGestureRecognitionListener.Stub
							.asInterface(gestureListenerStub));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		recognitionService = null;
		getActivity().unbindService(serviceConnection);
		
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
    
    
    //Initialization of media player and UI components 
    private void init()
    {
      mWalkingPlayer.setLooping(true);
      mWalkingPlayer.start();
      
      finalTime = mWalkingPlayer.getDuration();
      startTime = mWalkingPlayer.getCurrentPosition();

      musicSeekBar.setMax((int)finalTime);
      musicSeekBar.setProgress((int) startTime);
      musicSeekBar.setScrollbarFadingEnabled(true);
      myHandler.postDelayed(UpdateSongTime,100);
    }

    //Recursive runnable to update seekbar with song progression
	private Runnable UpdateSongTime = new Runnable() 
	{
	    public void run() 
	    {
		    if(mWalkingPlayer != null)
		    {
		    	if(seekBarMoving == false)
		    	{
		    		startTime = mWalkingPlayer.getCurrentPosition();
		    		musicSeekBar.setProgress((int)startTime);
		    		myHandler.postDelayed(this, 100);
		    	}
		    	else
		    	{
		    		myHandler.postDelayed(this, 100);
		    	}
		    }
	    }
	};
    private void cleanUp()
    {
      if (mWalkingPlayer != null)
      {
    	  if(!mWalkingPlayer.isPlaying())
    	  {
    		  mWalkingPlayer.release();
    	      mWalkingPlayer = null;
    	      if(visualizerCheck)
    	      {
    	    	  mVisualizerView.clearAnimation();
    	    	  mVisualizerView.clearRenderers();
    	    	  mVisualizerView.release();
    	      }
    	  }
    	  else
    	  {
    		  mVisualizerView.clearAnimation();
    		  mVisualizerView.clearRenderers();
    		  mVisualizerView.release();
    	  }
        
      }
      
      if (mSilentPlayer != null)
      {
    	  if(!mSilentPlayer.isPlaying())
    	  {
    		  mSilentPlayer.release();
    		  mSilentPlayer = null;
    		  mVisualizerView.clearAnimation();
    		  mVisualizerView.clearRenderers();
    		  mVisualizerView.release();
    	  }
    	  else
    	  {
    		  mVisualizerView.clearAnimation();
    		  mVisualizerView.clearRenderers();
    		  mVisualizerView.release();
    	  }
      }
    }
    
    // Workaround (for Galaxy S4)
    //
    // "Visualization does not work on the new Galaxy devices"
    //    
    //
    // NOTE: 
    //   This code is not required for visualizing default "test.mp3" file,
    //   because tunnel player is used when duration is longer than 1 minute.
    //   (default "test.mp3" file: 8 seconds)
    //
    private void initTunnelPlayerWorkaround() {
      // Read "tunnel.decode" system property to determine
      // the workaround is needed
      if (TunnelPlayerWorkaround.isTunnelDecodeEnabled(getActivity())) {
        mSilentPlayer = TunnelPlayerWorkaround.createSilentMediaPlayer(getActivity());
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
			if(mWalkingPlayer.isPlaying())
			{
				if(artistDisplayed)
				{
					artistNameText.setText(songData[3].toString());
					artistDisplayed = false;
					
				}
				else
				{
					artistNameText.setText(songData[1].toString());
					artistDisplayed = true;
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