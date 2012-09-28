package fm.ex.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import org.apache.cordova.api.Plugin;
import org.apache.cordova.api.PluginResult;
import org.apache.cordova.api.PluginResult.Status;


public class MediaStorePlugin extends Plugin{
	private static final Uri ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart");
	private String unknown;
	public static final String ACTION_GET_ALBUMS = "getAlbums";
	public static final String ACTION_GET_SONGS = "getSongs";
	public static final String ACTION_GET_ARTISTS = "getArtists";
	public static final String ACTION_GET_GENRES = "getGenres";
	
	public static final String ACTION_GET_ALBUMS_BY_ARTIST = "getAlbumsByArtist";
	public static final String ACTION_GET_ARTISTS_BY_GENRE = "getArtistsByGenre";
	public static final String ACTION_GET_SONGS_BY_ALBUM = "getSongsByAlbum";
	
	
	@Override
	public PluginResult execute(String action, JSONArray data, String callbackId) {
		if(unknown == null){
			unknown = (android.os.Build.VERSION.SDK_INT > 7) ? MediaStore.UNKNOWN_STRING : "<unknown>";
		}
		if(ACTION_GET_ALBUMS.equals(action)){
			return this.getAlbums();
		}
		else if(ACTION_GET_ALBUMS_BY_ARTIST.equals(action)){
			try {
				return this.getAlbumsByArtist(data.getInt(0));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		else if(ACTION_GET_ARTISTS.equals(action)){
			return this.getArtists();
		}
		
		else if(ACTION_GET_ARTISTS_BY_GENRE.equals(action)){
			try {
				return this.getArtistsByGenre(data.getInt(0));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		else if(ACTION_GET_GENRES.equals(action)){
			return this.getGenres();
		}
		else if(ACTION_GET_SONGS.equals(action)){
			return this.getSongs();
		}
		else if(ACTION_GET_SONGS_BY_ALBUM.equals(action)){
			try {
				return this.getSongsByAlbum(data.getInt(0));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	private PluginResult getAlbums(){
		
		final ContentResolver resolver = this.cordova.getContext().getContentResolver();
		final Cursor cursor = resolver.query(
				MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, null, null, null, 
				MediaStore.Audio.Albums.ALBUM);
		final JSONArray albums = new JSONArray();
		
		if (cursor != null && cursor.moveToFirst()) {
			do {
				final int idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID);
				final int nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM);
				final int artistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST);
				final JSONObject album = new JSONObject();
				final Integer id = cursor.getInt(idIdx);
				try {
					album.put("id", id);
					album.put("artist", cursor.getString(artistIdx));
					album.put("title", cursor.getString(nameIdx));
					album.put("image", getCoverArtFileForAlbumId(id));
					
					albums.put(album);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} while (cursor.moveToNext());
		}
		
		return new PluginResult(Status.OK, albums);	
	}
	
	private PluginResult getSongs(){
		final JSONArray songs = new JSONArray();
		final ContentResolver resolver = this.cordova.getContext().getContentResolver();
		final Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				null,
				null,
				null,
				MediaStore.Audio.Media.TITLE);

		if (cursor != null && cursor.moveToFirst()) {
			do {
				final int idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
				final int albumIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
				final int artistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
				final int titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
				final int dataIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
				final int albumIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
				final JSONObject song = new JSONObject();
				try{
					song.put("id", cursor.getInt(idIdx));
					song.put("album", cursor.getString(albumIdx));
					song.put("artist", cursor.getString(artistIdx));
					song.put("title", cursor.getString(titleIdx));
					song.put("url", cursor.getString(dataIdx));
					song.put("image", getCoverArtFileForAlbumId(cursor.getInt(albumIdIdx)));
					songs.put(song);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			} while (cursor.moveToNext());
		}

		return new PluginResult(Status.OK, songs);
	}
	private PluginResult getArtists(){
		final JSONArray artists = new JSONArray();
		final ContentResolver resolver = this.cordova.getContext().getContentResolver();
		final Cursor cursor = resolver.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
				null,
				null,
				null,
				MediaStore.Audio.Artists.ARTIST);

		if (cursor != null && cursor.moveToFirst()) {
			do {
				final int idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID);
				final int nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST);
				final JSONObject artist = new JSONObject();
				try{
					artist.put("id", cursor.getInt(idIdx));
					artist.put("name", cursor.getString(nameIdx));
					artists.put(artist);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			} while (cursor.moveToNext());
		}

		return new PluginResult(Status.OK, artists);
	}
	private PluginResult getGenres(){
		final JSONArray genres = new JSONArray();
		final ContentResolver resolver = this.cordova.getContext().getContentResolver();
		final Cursor cursor = resolver.query(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
				null,
				null,
				null,
				MediaStore.Audio.Genres.NAME);

		if (cursor != null && cursor.moveToFirst()) {
			do {
				final int idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID);
				final int nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME);
				final JSONObject genre = new JSONObject();
				try{
					genre.put("id", cursor.getInt(idIdx));
					genre.put("name", cursor.getString(nameIdx));
					genres.put(genre);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} while (cursor.moveToNext());
		}

		return new PluginResult(Status.OK, genres);
	}
	private PluginResult getAlbumsByArtist(final int artistId){
		final JSONArray albums = new JSONArray();
		
		final ContentResolver resolver = this.cordova.getContext().getContentResolver();
		final Cursor cursor = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
				null,
				"artist_id = " + artistId,
				null,
				MediaStore.Audio.Albums.ALBUM);

		if (cursor != null && cursor.moveToFirst()) {
			do {
				final int idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID);
				final Integer albumId = cursor.getInt(idIdx);
				
				final int nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM);
				final int artistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST);
				final JSONObject album = new JSONObject();
				try{
					album.put("id", albumId);
					album.put("artist", cursor.getString(artistIdx));
					album.put("title", cursor.getString(nameIdx));
					album.put("image", getCoverArtFileForAlbumId(albumId));
					albums.put(albumId, album);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} while (cursor.moveToNext());
		}
		return new PluginResult(Status.OK, albums);	
	}
	private PluginResult getArtistsByGenre(final int genreId){
		final JSONArray artists = new JSONArray();
		final ContentResolver resolver = this.cordova.getContext().getContentResolver();
		final Cursor cursor = resolver.query(buildGenreURI(genreId),
				null,
				null,
				null,
				MediaStore.Audio.Media.ARTIST);

		if (cursor != null && cursor.moveToFirst()) {
			do {
				final int idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID);
				final int nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
				final Integer artistId = cursor.getInt(idIdx);
				try{
					final JSONObject artist = new JSONObject();
					artist.put("id", artistId);
					artist.put("name", cursor.getString(nameIdx));
					artists.put(artist);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			} while (cursor.moveToNext());
		}

		return new PluginResult(Status.OK, artists);
	}
	private PluginResult getSongsByAlbum(final int albumId){
		final JSONArray songs = new JSONArray();
		final ContentResolver resolver = this.cordova.getContext().getContentResolver();
		final Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				null,
				MediaStore.Audio.Media.ALBUM_ID + " = " + albumId,
				null,
				MediaStore.Audio.Media.TRACK);

		if (cursor != null && cursor.moveToFirst()) {
			do {
				final int idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
				final int albumIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
				final int artistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
				final int titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
				final int dataIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
				final int albumIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
				final JSONObject song = new JSONObject();
				try{
					song.put("id", cursor.getInt(idIdx));
					song.put("album", cursor.getString(albumIdx));
					song.put("artist", cursor.getString(artistIdx));
					song.put("title", cursor.getString(titleIdx));
					song.put("url", cursor.getString(dataIdx));
					song.put("image", getCoverArtFileForAlbumId(cursor.getInt(albumIdIdx)));
					songs.put(song);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			} while (cursor.moveToNext());
		}

		return new PluginResult(Status.OK, songs);
	}
	
	private String getCoverArtFileForAlbumId(final Integer albumId) {
		if (albumId != null) {
			final Uri uri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId);

			return uri.toString();
		}
		return null;
	}
	
	private Uri buildGenreURI(final Integer genreId) {
		final StringBuilder uri = new StringBuilder();

		uri.append(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI.toString());
		uri.append("/");
		uri.append(genreId);
		uri.append("/");
		uri.append(MediaStore.Audio.Genres.Members.CONTENT_DIRECTORY);
		return Uri.parse(uri.toString());
	}
}