package fm.ex.android;

import org.apache.cordova.api.Plugin;
import org.apache.cordova.api.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class IEXAudio extends Plugin{
	private static final String TAG = "IEXAudio";
	public boolean interrupted;
	// time observer?
	
	public String url;
	public String callback;
	public IntentFilter playerIntentFilter;
	
	public static Context context;
	
	public static Context getContext(){
		return context;
	}
	
	private JSONObject intentExtrasToJSON(Intent intent){
		JSONObject data = new JSONObject();
		try{
			if(intent.hasExtra(IEXAudioService.NAME)){
				data.put("name", intent.getStringExtra(IEXAudioService.NAME));
			}
			
			if(intent.getStringExtra(IEXAudioService.NAME) == IEXAudioService.Events.PROGRESS){
				final JSONArray ranges = new JSONArray();
				final JSONObject range = new JSONObject();
				
				range.put("start", intent.getIntExtra(IEXAudioService.START, 0));
				range.put("duration", intent.getFloatExtra(IEXAudioService.DURATION, 0));
				ranges.put(0, range);
				data.put("ranges", ranges);
			}
			else{
				if(intent.hasExtra(IEXAudioService.DURATION)){
					data.put("duration", intent.getFloatExtra(IEXAudioService.DURATION, 0));
				}
			}
			
			if(intent.hasExtra(IEXAudioService.CURRENT_TIME)){
				data.put("currentTime", intent.getIntExtra(IEXAudioService.CURRENT_TIME, 0));
			}
			
			if(intent.hasExtra(IEXAudioService.METHOD)){
				data.put("method", intent.getStringExtra(IEXAudioService.METHOD));
			}
			
			if(intent.hasExtra(IEXAudioService.MESSAGE)){
				data.put("message", "" + intent.getIntExtra(IEXAudioService.MESSAGE, 0));
			}
			
			if(intent.hasExtra(IEXAudioService.CODE)){
				data.put("code", "" + intent.getIntExtra(IEXAudioService.CODE, 0));
			}
			
		} catch(Exception e){
			e.printStackTrace();
		}
		return data;
	}
	
	private BroadcastReceiver playerEventListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final JSONObject data = intentExtrasToJSON(intent);
		
			PluginResult result;
			if(intent.getStringExtra(IEXAudioService.NAME) == IEXAudioService.ERROR){
				result = new PluginResult(PluginResult.Status.ERROR, data);
				result.setKeepCallback(true);
				error(result, callback);
			}
			else{
				result = new PluginResult(PluginResult.Status.OK, data);
				result.setKeepCallback(true);
				success(result, callback);
			}
			
		}
	};
	
	@Override
	public PluginResult execute(String action, JSONArray data, String callbackId) {
		context = this.cordova.getContext();
		
		if(playerIntentFilter == null){
			playerIntentFilter = new IntentFilter();
			playerIntentFilter.addAction(IEXAudioService.EVENT);
			context.registerReceiver(playerEventListener, 
					playerIntentFilter);
		}
		try{
			
			if(action.equals("eventHandler")){
				this.addEventHandler(callbackId);
				final PluginResult result = new PluginResult(PluginResult.Status.OK);
				result.setKeepCallback(true);
				return result;
			}
			else if(action.equals("setSource")){
				setSource(data.getString(0));
				return new PluginResult(PluginResult.Status.OK);
			}
			else if(action.equals("load")){
				load();
				return new PluginResult(PluginResult.Status.OK);
			}
			else if(action.equals("play")){
				play();
				return new PluginResult(PluginResult.Status.OK);
			}
			else if(action.equals("pause")){
				pause();
				return new PluginResult(PluginResult.Status.OK);
			}
			else if(action.equals("stop")){
				stop();
				return new PluginResult(PluginResult.Status.OK);
			}
			else if(action.equals("seek")){
				seek(data.getInt(0));
				return new PluginResult(PluginResult.Status.OK);
			}
			else if(action.equals("nowPlaying")){
				setNowPlaying(data.getString(0), data.getString(1), data.getString(2), data.getString(3));
				return new PluginResult(PluginResult.Status.OK);
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public void setSource(final String url){
		Intent intent = new Intent(IEXAudioService.SET_SOURCE);
		intent.putExtra(IEXAudioService.URL, url);
		context.startService(intent);
		
		this.url = url;
	}
	
	public void load(){
		Intent intent = new Intent(IEXAudioService.LOAD);
		context.startService(intent);
	}
	
	public void play(){
		Intent intent = new Intent(IEXAudioService.PLAY);
		context.startService(intent);
	}
	
	public void pause(){
		Intent intent = new Intent(context, IEXAudioService.class);
		intent.setAction(IEXAudioService.PAUSE);
		context.startService(intent);
	}
	
	public void stop(){
		Intent intent = new Intent(context, IEXAudioService.class);
		intent.setAction(IEXAudioService.STOP);
		context.startService(intent);
	}
	
	public void seek(final int position){
		Intent intent = new Intent(context, IEXAudioService.class);
		intent.setAction(IEXAudioService.SEEK);
		intent.putExtra(IEXAudioService.SEEK_POSITION, (position * 1000));
		context.startService(intent);
	}
	
	public void setNowPlaying(final String artist, final String title, final 
			String album, final String imageUrl){
		
		Intent intent = new Intent(context, IEXAudioService.class);
		intent.setAction(IEXAudioService.SET_NOW_PLAYING);
		intent.putExtra(IEXAudioService.ARTIST, artist);
		intent.putExtra(IEXAudioService.ALBUM, album);
		intent.putExtra(IEXAudioService.TITLE, title);
		intent.putExtra(IEXAudioService.IMAGE_URL, imageUrl);
		context.startService(intent);
	}
	
	public void addEventHandler(final String callbackId){
		this.callback = callbackId;
	}
}
