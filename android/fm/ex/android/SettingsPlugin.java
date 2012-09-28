package fm.ex.android;
import org.apache.cordova.api.Plugin;
import org.apache.cordova.api.PluginResult;
import org.apache.cordova.api.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SettingsPlugin extends Plugin {
	private static final String TAG = "SettingsPlugin";
	public static final String ACTION_GET_STRING = "getString";
	public static final String ACTION_SET_STRING = "setString";
	
	@Override
	public PluginResult execute(String action, JSONArray data, String callbackId) {
		Log.d(TAG, "execute called with action " + action);
		
		try{
			if(action.equals(ACTION_GET_STRING)){
				Log.d(TAG, "Its get setting string ");
				return new PluginResult(Status.OK, 
						getSettings().getString(data.getString(0), ""));
			}
			else if (action.equals(ACTION_SET_STRING)){
				final SharedPreferences.Editor editor = getSettings().edit();
				editor.putString(data.getString(0), data.getString(1));
				editor.commit();
				return new PluginResult(Status.OK);
			}
		} catch (JSONException e) {
			Log.e(TAG, "JSON Exception: "+ e.getMessage());
			return new PluginResult(Status.JSON_EXCEPTION);
		}
		Log.d(TAG, "Something wrong.  returning null");
		
		return null;
	}
	
	public SharedPreferences getSettings(){
		return cordova.getContext().getSharedPreferences("UserSettings", 
				Context.MODE_PRIVATE);
	}
}