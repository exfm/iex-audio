package fm.ex.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;

public class IEXAudioMediaButtonHandler extends BroadcastReceiver {
	private final static String TAG = "IEXAudioMediaButtonHandler";
	
	public static int numClicks = 0;
	
	private final Runnable onHeadseatClick = new Runnable() {
		public void run() {
			Log.d(TAG, "in run numClicks is "+numClicks);
			if(numClicks == 0){
				Log.w(TAG, "Bad state... numClicks is 0");
			}
			else if(numClicks == 3){
				Log.d(TAG, "calling prev");
				prev();
			}
			else if(numClicks == 2){
				Log.d(TAG, "calling next");
				next();
			}
			else{
				Log.d(TAG, "calling pause");
				pause();
			}
			Log.d(TAG, "Setting numClicks back to 0");
			numClicks = 0;
			Log.d(TAG, "Set numClicks to "+numClicks+". done.");
		}
	};
	
	public void play(){
		Intent intent = new Intent(IEXAudioService.PLAY);
		IEXAudio.getContext().startService(intent);
	}
	
	public void pause(){
		Intent intent = new Intent(IEXAudioService.PAUSE);
		IEXAudio.getContext().startService(intent);
	}
	
	public void next(){
		Intent intent = new Intent(IEXAudioService.REMOTE_NEXT);
		IEXAudio.getContext().startService(intent);
	}
	
	public void prev(){
		Intent intent = new Intent(IEXAudioService.REMOTE_PREV);
		IEXAudio.getContext().startService(intent);
	}
	
	private Handler onHeadseatClickHandler = new Handler();
	

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "onReceieve called");

		if (intent.getAction().equals("android.intent.action.MEDIA_BUTTON")) {
			KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

			if (event == null) {
				return;
			}

			int keycode = event.getKeyCode();

			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				switch (keycode) {
				case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
					prev();
					abortBroadcast();
					return;
					
				case KeyEvent.KEYCODE_MEDIA_NEXT:
					next();
					abortBroadcast();
					return;
					
				case KeyEvent.KEYCODE_HEADSETHOOK:
					onHeadseatClickHandler.removeCallbacks(onHeadseatClick);
					onHeadseatClickHandler.removeMessages(0);
					
					numClicks = numClicks + 1;
					Log.d(TAG, "numClicks now "+numClicks+".  calling post delayed");
					onHeadseatClickHandler.postDelayed(onHeadseatClick, 400);
					abortBroadcast(); // Abort broadcast so voice dialer doesn't mess with us.
					return;
					
					
				case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
					pause();
					abortBroadcast();
					return;
				}
			}
		}
	}
}
