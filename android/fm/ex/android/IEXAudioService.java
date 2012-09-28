package fm.ex.android;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.RemoteControlClient;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import fm.ex.android.RemoteControlClientCompat.MetadataEditorCompat;

public class IEXAudioService extends Service 
	implements OnBufferingUpdateListener, OnCompletionListener, OnErrorListener,
		OnPreparedListener, MusicFocusable {
	
	private static final String TAG = "IEXAudioService";
	
	public enum PlayerStatus {
		Playing, 
		Paused, 
		Buffering, 
		Stopped;
	}
	
	public enum MediaState {
		Nothing,
	    CurrentData,
	    FutureData,
	    EnoughData;
	}
	
	public enum NetworkState{
	    Empty,
	    Idle,
	    Loading,
	    NoSource;
	}
	
	public class Events{
		public static final String DURATION_CHANGED = "durationchanged";
		public static final String CAN_PLAY = "canplay";
		public static final String CAN_PLAY_THROUGH = "canplaythrough";
		public static final String PAUSED = "paused";
		public static final String PROGRESS = "progress";
		public static final String ENDED = "ended";
		public static final String ERROR = "error";
		public static final String PLAY = "play";
		public static final String TIME_UPDATED = "timeupdate";
		public static final String LOAD_START = "loadstart";
		public static final String REMOTE_NEXT = "remotenext";
		public static final String REMOTE_PREVIOUS = "remoteprevious";
	}	
	
	public static final String PLAY = "fm.ex.android.play";
	public static final String STOP = "fm.ex.android.stop";
	public static final String SEEK = "fm.ex.android.seek";
	public static final String PAUSE = "fm.ex.android.pause";
	public static final String LOAD = "fm.ex.android.load";
	public static final String REMOTE_NEXT = "fm.ex.android.remotenext";
	public static final String REMOTE_PREV = "fm.ex.android.remoteprev";
	public static final String SET_SOURCE = "fm.ex.android.setsource";
	public static final String SET_NOW_PLAYING = "fm.ex.android.setnowplaying";
	
	public static final String URL = "fm.ex.android.url";
	public static final String SEEK_POSITION = "fm.ex.android.seekposition";
	public static final String ARTIST = "fm.ex.android.artist";
	public static final String ALBUM = "fm.ex.android.album";
	public static final String TITLE = "fm.ex.android.title";
	public static final String IMAGE_URL = "fm.ex.android.imageurl";
	public static final String EVENT = "fm.ex.android.event";
	public static final String NAME = "fm.ex.android.name";
	public static final String MESSAGE = "fm.ex.android.message";
	public static final String CODE = "fm.ex.android.code";
	public static final String METHOD = "fm.ex.android.method";
	public static final String ERROR = "fm.ex.android.error";
	public static final String BUFFER_PROGRESS = "fm.ex.android.bufferprogress";
	public static final String START = "fm.ex.android.start";
	public static final String DURATION = "fm.ex.android.duration";
	public static final String CURRENT_TIME = "fm.ex.android.currenttime";
	
	public PlayerStatus status;
	public MediaState readyState;
	public NetworkState networkState;
	
	public MediaPlayer player;
	public int duration;
	public AudioManager mAudioManager;
	public boolean canPlaySent = false;
	private PowerManager.WakeLock wakeLock;
	private WifiManager.WifiLock wifiLock;
	private boolean pausedForHeadphoneUnplug = false;
	public FadeVolumeTask mFadeVolumeTask = null;
    public RemoteControlClientCompat mRemoteControlClientCompat;
	public TelephonyManager mTelephonyManager;
	public boolean focusLost = false;
	public boolean hasFocus = false;
	private final float DUCK_VOLUME = 0.1f;
    private MusicPlayerFocusHelper mFocusHelper;
    
    public String url;
    
    private final Handler heartbeatHandler = new Handler();
    
    private final Runnable heartbeat = new Runnable() {
		public void run() {
			if(status == PlayerStatus.Playing){
				if(player != null && player.isPlaying()){
					Intent intent = new Intent(EVENT);
					intent.putExtra(NAME, Events.TIME_UPDATED);
					intent.putExtra(CURRENT_TIME, player.getCurrentPosition() / 1000);
					sendBroadcast(intent);
				}
			}
			heartbeatHandler.postDelayed(heartbeat, 1000);
		}
	};
	
	public IEXAudioService() {
		super();
	}
	
	@Override
	public void onCreate(){
		super.onCreate();
		
		initializeStaticCompatMethods();
		
        mFocusHelper = new MusicPlayerFocusHelper(this.getApplicationContext(), this);
        
        accquireLocks();
        
//		if(!mFocusHelper.isSupported()) {
//			mTelephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
//			mTelephonyManager.listen(new PhoneStateListener() {
//				@Override
//				public void onCallStateChanged(int state, String incomingNumber) {
//					if (status != PlayerStatus.Stopped) {
//						if (state == TelephonyManager.CALL_STATE_IDLE) {
//							focusGained();
//						} else { // fade music out to silence
//							focusLost(true, false);
//						}
//					}
//					super.onCallStateChanged(state, incomingNumber);
//				}
//			}, PhoneStateListener.LISTEN_CALL_STATE);
//		}
//		
		registerReceiver(headsetUnplugReceiver, 
				new IntentFilter(Intent.ACTION_HEADSET_PLUG));
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(IEXAudioService.SET_SOURCE);
		filter.addAction(IEXAudioService.LOAD);
		
		registerReceiver(intentReceiver, filter);
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "Player service shutting down");
		try {
			if (player != null) {
				if (player.isPlaying()){
					player.stop();
				}
				player.release();
				player = null;
			}
		} 
		catch (Exception e) {}
		
		unregisterReceiver(intentReceiver);
		unregisterReceiver(headsetUnplugReceiver);
		releaseLocks();
//		heartbeatHandler.removeCallbacks(heartbeat);
	}
	
	private BroadcastReceiver intentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			onHandleIntent(intent);
		}
	};
	
	public void onHandleIntent(Intent intent) {
		synchronized (this) {
            String action = intent.getAction();
            if(action.equals(SET_SOURCE)){
            	setSource(intent.getStringExtra(URL));
            }
            else if(action.equals(LOAD)){
            	load();
            }
            else if(action.equals(PLAY)){
            	play();
            }
            else if(action.equals(PAUSE)){
            	pause();
            }
            else if(action.equals(STOP)){
        		stop();
            }
            else if(action.equals(SEEK)){
            	seek(intent.getIntExtra(IEXAudioService.SEEK_POSITION, 0));
            }
            else if(action.equals(SET_NOW_PLAYING)){
        		setNowPlaying(intent.getStringExtra(IEXAudioService.ARTIST),
        			intent.getStringExtra(IEXAudioService.TITLE), 
        			intent.getStringExtra(IEXAudioService.ALBUM),
        			intent.getStringExtra(IEXAudioService.IMAGE_URL));
            }
        }
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		onHandleIntent(intent);
	    return START_STICKY;
	}
	
	////////////////////////////////////////////////////////////////////////////
	// Main API
	////////////////////////////////////////////////////////////////////////////
	public boolean isPlaying() {
		return player != null && player.isPlaying();
	}
	
	public void seek(final int position){
		player.seekTo(position);
	}
	
	public void setSource(final String url){
		try {
			getPlayer();
			if(this.url != url){
				player.reset();
				player.setDataSource(url);
				this.url = url;
			}
			Log.d(TAG, "Set source to "+url);
			networkState = NetworkState.Empty;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void load(){
		Log.d(TAG, "Load called");
		canPlaySent = false;
		getPlayer();
		
    	player.setOnCompletionListener(this);
    	player.setAudioStreamType(AudioManager.STREAM_MUSIC);
    	player.setOnBufferingUpdateListener(this);
    	player.setOnPreparedListener(this);
    	player.setOnErrorListener(this);
    	player.prepareAsync();
    	status = PlayerStatus.Buffering;
    	
        registerMediaButtonEventReceiverCompat(mAudioManager, 
        		new ComponentName(getApplicationContext(), IEXAudioMediaButtonHandler.class));

        if (mFocusHelper.isSupported()) {
            mFocusHelper.requestMusicFocus();
            hasFocus = true;
        }

        // Use the remote control APIs (if available) to set the playback state

        if (mRemoteControlClientCompat == null) {
            Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            intent.setComponent(new ComponentName(this, IEXAudioMediaButtonHandler.class));
            mRemoteControlClientCompat = new RemoteControlClientCompat(
                    PendingIntent.getBroadcast(this /*context*/,
                            0 /*requestCode, ignored*/, intent /*intent*/, 0 /*flags*/));
            RemoteControlHelper.registerRemoteControlClient(mAudioManager,
                    mRemoteControlClientCompat);
        }       
		player.setVolume(1.0f, 1.0f);
	}
	
	public void stop(){
		if (player != null) {
			try {
				player.stop();
				player.release();
				player = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		status = PlayerStatus.Stopped;
		releaseLocks();
		
        if (mFocusHelper.isSupported()) {
            mFocusHelper.abandonMusicFocus();
            hasFocus = false;
        }
        if (mRemoteControlClientCompat != null){
            mRemoteControlClientCompat.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
        }
	}
	
	public void play(){
		Log.d(TAG, "Play called");
		player.start();
		status = PlayerStatus.Playing;
		heartbeatHandler.postDelayed(heartbeat, 0);
    	publishEvent(Events.PLAY);
	}
	
	public void pause(){
    	player.pause();
    	status = PlayerStatus.Paused;
		
		releaseLocks();
		
		if (mRemoteControlClientCompat != null){
            mRemoteControlClientCompat.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
        }
    	publishEvent(Events.PAUSED);
	}
	
	public void setNowPlaying(final String artist, final String title, final 
			String album, final String imageUrl){
        final SetMediaPlayerInfoTask task = new SetMediaPlayerInfoTask(
        		artist, title, album, imageUrl, mRemoteControlClientCompat);
		task.execute();
	}
	
	public MediaPlayer getPlayer(){
		if(player == null){
			player = new MediaPlayer();
		}
		return player;
	}
	
	////////////////////////////////////////////////////////////////////////////
	// MediaPlayer event handling
	////////////////////////////////////////////////////////////////////////////
	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		if(!canPlaySent){
			final float durationAvailable = (float) duration/ (float) 1000 * ((float) percent / (float) 100);
			Intent intent = new Intent(EVENT);
			intent.putExtra(NAME, BUFFER_PROGRESS);
			intent.putExtra(START, 0);
			intent.putExtra(DURATION, durationAvailable);
			this.sendBroadcast(intent);
			if(percent == 100){
				publishEvent(Events.CAN_PLAY_THROUGH);
				canPlaySent = true;
			}
		}
	} 
	
	@Override public void onCompletion(MediaPlayer p) {
		publishEvent(Events.ENDED);
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		duration = player.getDuration();
		publishEvent(Events.CAN_PLAY);
		
		Intent intent = new Intent(EVENT);
		intent.putExtra(NAME, Events.DURATION_CHANGED);
		intent.putExtra(DURATION, (float) player.getDuration() / 1000);
		this.sendBroadcast(intent);
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra){
		publishError(what, extra);
		return false;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// Talk back to PhoneGap plugin
	////////////////////////////////////////////////////////////////////////////
	public void publishEvent(final String name){
		Intent intent = new Intent(EVENT);
		intent.putExtra(NAME, name);
		this.sendBroadcast(intent);
	}
	
	public void publishError(final int message, final int code){
		Intent intent = new Intent(EVENT);
		intent.putExtra(METHOD, Events.ERROR);
		intent.putExtra(NAME, ERROR);
		intent.putExtra(MESSAGE, message);
		intent.putExtra(CODE, code);
		this.sendBroadcast(intent);
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// Locking
	////////////////////////////////////////////////////////////////////////////
	private void accquireLocks(){
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "exfm player service");
		
		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		
		WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifiLock = wm.createWifiLock(((Integer.decode(Build.VERSION.SDK) > 8) ?  
			WifiManager.WIFI_MODE_FULL_HIGH_PERF : WifiManager.WIFI_MODE_FULL), 
				"exfm player service");
	}
	
	private void releaseLocks() {
		if(wakeLock != null && wakeLock.isHeld()){
			wakeLock.release();
		}
		
		if(wifiLock != null && wifiLock.isHeld()){
			wifiLock.release();
		}
	}

	// Backwards compatibility code (methods available as of SDK Level 8)
    static {
        initializeStaticCompatMethods();
    }

    static Method sMethodRegisterMediaButtonEventReceiver;
    static Method sMethodUnregisterMediaButtonEventReceiver;

    private static void initializeStaticCompatMethods() {
        try {
            sMethodRegisterMediaButtonEventReceiver = AudioManager.class.getMethod(
                    "registerMediaButtonEventReceiver",
                    new Class[] { ComponentName.class });
            sMethodUnregisterMediaButtonEventReceiver = AudioManager.class.getMethod(
                    "unregisterMediaButtonEventReceiver",
                    new Class[] { ComponentName.class });
        } catch (NoSuchMethodException e) {
            // Silently fail when running on an OS before SDK level 8.
        }
    }

    private static void registerMediaButtonEventReceiverCompat(AudioManager audioManager,
            ComponentName receiver) {
        if (sMethodRegisterMediaButtonEventReceiver == null)
            return;

        try {
            sMethodRegisterMediaButtonEventReceiver.invoke(audioManager, receiver);
        } catch (InvocationTargetException e) {
            // Unpack original exception when possible
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                // Unexpected checked exception; wrap and re-throw
                throw new RuntimeException(e);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    private static void unregisterMediaButtonEventReceiverCompat(AudioManager audioManager,
            ComponentName receiver) {
        if (sMethodUnregisterMediaButtonEventReceiver == null)
            return;

        try {
            sMethodUnregisterMediaButtonEventReceiver.invoke(audioManager, receiver);
        } catch (InvocationTargetException e) {
            // Unpack original exception when possible
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                // Unexpected checked exception; wrap and re-throw
                throw new RuntimeException(e);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    
	public void focusGained() {
		hasFocus = true;
		
		if (mFadeVolumeTask != null)
			mFadeVolumeTask.cancel();

		if(status == PlayerStatus.Paused && focusLost) {
			Log.d(TAG, "fading music back in");
			mFadeVolumeTask = new FadeVolumeTask(FadeVolumeTask.FADE_IN, 5000) {
				@Override
				public void onPreExecute() {
					if(player != null){
						player.setVolume(0.0f, 0.0f);
					}
					if (status == PlayerStatus.Paused){
						play();
					}
				}

				@Override
				public void onPostExecute() {
					mFadeVolumeTask = null;
				}
			};
			focusLost = false;
		} else {
			try {
				if(isPlaying()){
					player.setVolume(1.0f, 1.0f);
				}
			} catch (Exception e) { //Sometimes the MediaPlayer is in a state where isPlaying() or setVolume() will fail
				e.printStackTrace();
			}
		}
	}

	public void focusLost(boolean isTransient, boolean canDuck) {
		hasFocus = false;

		if(status == PlayerStatus.Stopped || status == PlayerStatus.Paused)
			return;
		
		if (mFadeVolumeTask != null)
			mFadeVolumeTask.cancel();

		if (player == null || status == PlayerStatus.Paused)
            return;

        if (canDuck) {
    		try {
    			player.setVolume(DUCK_VOLUME, DUCK_VOLUME);
    		} catch (Exception e) { //Sometimes the MediaPlayer is in a state where setVolume() will fail
    			e.printStackTrace();
    		}
        } else {
			Log.d(TAG, "fading music out");

			mFadeVolumeTask = new FadeVolumeTask(FadeVolumeTask.FADE_OUT, 1500) {
				@Override
				public void onPreExecute() {
				}

				@Override
				public void onPostExecute() {
					pause();
					if(player != null)
						player.setVolume(1.0f, 1.0f); //Resture the volume in case the user un-pauses radio manually
					mFadeVolumeTask = null;
				}
			};
            focusLost = isTransient;
        }
	}
	
	private final BroadcastReceiver headsetUnplugReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();
			// If we're playing and we unplug our headphones, pause.
			if ((action.compareTo(Intent.ACTION_HEADSET_PLUG)) == 0) {
				final int headSetState = intent.getIntExtra("state", 0);

				if (headSetState == 0) {
					if (isPlaying()){
						pause();
						pausedForHeadphoneUnplug = true;
					}
				}
				else {
					if(pausedForHeadphoneUnplug){
						play();
						pausedForHeadphoneUnplug = false;
					}
				}
			}
		}
	};
	
	
	
	private abstract class FadeVolumeTask extends TimerTask {

		public static final int FADE_IN = 0;
		public static final int FADE_OUT = 1;

		private int mCurrentStep = 0;
		private int mSteps;
		private int mMode;

		/**
		 * Constructor, launches timer immediately
		 * 
		 * @param mode
		 *            Volume fade mode <code>FADE_IN</code> or
		 *            <code>FADE_OUT</code>
		 * @param millis
		 *            Time the fade process should take
		 * @param steps
		 *            Number of volume gradations within given fade time
		 */
		public FadeVolumeTask(int mode, int millis) {
			this.mMode = mode;
			this.mSteps = millis / 20; // 20 times per second
			this.onPreExecute();
			new Timer().scheduleAtFixedRate(this, 0, millis / mSteps);
		}

		@Override
		public void run() {
			float volumeValue = 1.0f;

			if (mMode == FADE_OUT) {
				volumeValue *= (float) (mSteps - mCurrentStep) / (float) mSteps;
			} else {
				volumeValue *= (float) (mCurrentStep) / (float) mSteps;
			}

			try {
				player.setVolume(volumeValue, volumeValue);
			} catch (Exception e) {
				return;
			}

			if (mCurrentStep >= mSteps) {
				this.onPostExecute();
				this.cancel();
			}

			mCurrentStep++;
		}

		/**
		 * Task executed before launching timer
		 */
		public abstract void onPreExecute();

		/**
		 * Task executer after timer finished working
		 */
		public abstract void onPostExecute();
	}
	
	private static class SetMediaPlayerInfoTask extends AsyncTask<Void, Integer, Void> { 
		private RemoteControlClientCompat mRemoteControlClientCompat;
		public String artist;
		public String title;
		public String album;
		public String imageUrl;
		
		public SetMediaPlayerInfoTask(final String artist, final String title, final 
				String album, final String imageUrl, 
				final RemoteControlClientCompat mRemoteControlClientCompat) {
			
			this.artist = artist;
			this.title = title;
			this.album = album;
			this.imageUrl = imageUrl;
			this.mRemoteControlClientCompat = mRemoteControlClientCompat;
		}

		@Override
		protected Void doInBackground(final Void... arg0) {
            mRemoteControlClientCompat.setPlaybackState(
                    RemoteControlClient.PLAYSTATE_PLAYING);

            mRemoteControlClientCompat.setTransportControlFlags(
                    RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                    RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                    RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                    RemoteControlClient.FLAG_KEY_MEDIA_STOP);

            // Update the remote controls
            MetadataEditorCompat editor = mRemoteControlClientCompat.editMetadata(true);
            
        	editor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, artist);
            editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, title);
            editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, album);
            if(imageUrl != null){
            	Bitmap albumArt = getImageFromUrl(imageUrl);
            	if(albumArt != null){
            		editor.putBitmap(RemoteControlClientCompat.MetadataEditorCompat.METADATA_KEY_ARTWORK, albumArt);
            	}
            }
            editor.apply();
			return null;
		}
		
		public static Bitmap getImageFromUrl(final String url){
			try{
				final URL endPoint = new URL(url);
				final HttpURLConnection connection = (HttpURLConnection) endPoint.openConnection();
				connection.setDoInput(true);
				connection.connect();
				BitmapFactory.Options o = new BitmapFactory.Options();
			    for (int i = 1; i<10; i++){
			        o.inSampleSize = i;
			        o.inJustDecodeBounds = false;
			        try{
			            return BitmapFactory.decodeStream(connection.getInputStream(), null, o);
			        }catch(OutOfMemoryError E){
			            Log.d(TAG,String.format("Out of memory.  Forcing GC..."));
			            System.gc();
			        }           
			    }
			    return null;
			}
			catch(Exception e){
				return null;
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
