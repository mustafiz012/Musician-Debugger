package musician.kuet.musta;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Playing extends ListActivity implements View.OnClickListener, MediaPlayer.OnCompletionListener {
	static boolean active = false, shuffleFlag = false, isRepeatOneOn = false, isRepeatOn = false;
	private static final String TAG = null;
	ListView songList;
	Button playingSong;
	int songPositionFromList = 0;
	Uri uri;
	String currentFile;
	Thread seekBarUpdating;
	long totalDuration = 0;
	long currentPosition = 0;
	FloatingActionButton floatingActionButton;
	SeekBar bar;
	ImageView preSong, nextSong, playPause;
	Button shuffle, repeat, nowPlayingSongs, goToSongList;
	TextView currentSong, leftDuration, rightDuration;
	private double startTime = 0;
	private double finalTime = 0;
	private Handler myHandler = new Handler();
	String currentItem = "";
	NowPlaying status = new NowPlaying();
	ArrayList<File> songs, songs2;
	int songsSize = 0;
	Random randomPosition = new Random();
	private MediaCursorAdapter mediaCursorAdapter = null;
	private MediaPlayer player = null;

	private void updatePlaylist() {
		Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
		if (null != cursor) {
			cursor.moveToFirst();
			//listing out the songList in Playing activity
			mediaCursorAdapter = new MediaCursorAdapter(getApplicationContext(), R.layout.song_layout, cursor);
			setListAdapter(mediaCursorAdapter);
			songsSize = cursor.getCount();
		}
	}

	public void initialization() {
		playingSong = (Button) findViewById(R.id.playingSong);
		floatingActionButton = (FloatingActionButton) findViewById(R.id.shuffle_all_songs);
		//floatingActionButton.setOnClickListener(this);
		TextView songsSize = (TextView) findViewById(R.id.songsSize);
		//playingSong.setOnClickListener(this);
		songList = (ListView) findViewById(android.R.id.list);
		//registerForContextMenu(songList);
		bar = (SeekBar) findViewById(R.id.seekBar);
		preSong = (ImageView) findViewById(R.id.previous);
		nextSong = (ImageView) findViewById(R.id.next);
//		leftSeek = (Button) findViewById(R.id.leftSeeking);
//		rightSeek = (Button) findViewById(R.id.rightSeeking);
		playPause = (ImageView) findViewById(R.id.playPause);
		currentSong = (TextView) findViewById(R.id.currentSong);
		leftDuration = (TextView) findViewById(R.id.leftDuration);
		rightDuration = (TextView) findViewById(R.id.rightDuration);
		goToSongList = (Button) findViewById(R.id.songList);
		shuffle = (Button) findViewById(R.id.shuffle);
		repeat = (Button) findViewById(R.id.repeat);
		nowPlayingSongs = (Button) findViewById(R.id.imageViewSongList);

		preSong.setOnClickListener(this);
		nextSong.setOnClickListener(this);
//		leftSeek.setOnClickListener(this);
//		rightSeek.setOnClickListener(this);
		playPause.setOnClickListener(this);
		goToSongList.setOnClickListener(this);
		shuffle.setOnClickListener(this);
		repeat.setOnClickListener(this);
		playingSong.setOnClickListener(this);
		//player.setOnCompletionListener(this);
		nowPlayingSongs.setOnClickListener(this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_playing);
		initialization();
		ArrayList<File> root = new ArrayList<File>();
		player = new MediaPlayer();

		Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);

		if (null != cursor) {
			cursor.moveToFirst();

			mediaCursorAdapter = new MediaCursorAdapter(this, R.layout.listitem, cursor);

			setListAdapter(mediaCursorAdapter);
		}

		//for updating the current song duration
		seekBarUpdating = new Thread() {
			@Override
			public void run() {
				totalDuration = player.getDuration();
				currentPosition = 0;
				while (currentPosition + 1001 < totalDuration) {
					try {
						sleep(1001);
						currentPosition = player.getCurrentPosition();
						bar.setProgress((int) currentPosition);
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalStateException e) {
						e.printStackTrace();
					}
				}
			}
		};
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		findViewById(R.id.now_playing_layout).setVisibility(View.VISIBLE);
		findViewById(R.id.parentLayout).setVisibility(View.GONE);
		Log.i("position", ""+position);
		currentItem = (String) v.getTag();

		startPlay(currentItem);
		songPositionFromList = position;
	}

	public void savePlayerStates(String key, boolean value) {
		SharedPreferences playerStates = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor playerStatesEditor = playerStates.edit();
		playerStatesEditor.putBoolean(key, value);
		playerStatesEditor.commit();
		Toast.makeText(getApplicationContext(), "Player States Saved", Toast.LENGTH_SHORT).show();
	}

	public void loadPlayerStates() {
		SharedPreferences playerStates = PreferenceManager.getDefaultSharedPreferences(this);
		boolean shuffleOn = playerStates.getBoolean("shuffleOn", false);
		boolean repeatOn = playerStates.getBoolean("repeatOn", false);
		boolean repeatOneOn = playerStates.getBoolean("repeatOneOn", false);

		if (shuffleOn) {
			shuffleFlag = true;
			shuffle.setBackgroundResource(R.mipmap.shuffle);
		} else {
			shuffleFlag = false;
			shuffle.setBackgroundResource(R.mipmap.shuffle_off);
		}
		if (repeatOn && !repeatOneOn) {
			isRepeatOn = true;
			isRepeatOneOn = false;
			repeat.setBackgroundResource(R.mipmap.repeat);
		} else if (repeatOneOn && !repeatOn) {
			isRepeatOneOn = true;
			isRepeatOn = false;
			repeat.setBackgroundResource(R.mipmap.repeat_one);
		} else if (!repeatOn && !repeatOneOn) {
			isRepeatOn = false;
			isRepeatOneOn = false;
			repeat.setBackgroundResource(R.mipmap.repeat_off);
		}
		Toast.makeText(getApplicationContext(), "Player States restored", Toast.LENGTH_SHORT).show();
	}

	private void startPlay(String file) {
		Log.i("Selected: ", file);

		currentSong.setText(file);
		bar.setProgress(0);

		player.stop();
		player.reset();

		try {
			player.setDataSource(file);
			player.prepare();
			player.start();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		bar.setMax(player.getDuration());
		playPause.setImageResource(R.drawable.btn_pause);
		updateSongInfo(songPositionFromList);
		//seekBarUpdating.start();
	}

	boolean doubleBackToExitPressedOnce = false;

	@Override
	public void onBackPressed() {
		if (doubleBackToExitPressedOnce) {
			moveTaskToBack(true);
		}
		if (findViewById(R.id.now_playing_layout).getVisibility() == View.GONE) {
			this.doubleBackToExitPressedOnce = true;
			Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
		}
		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				doubleBackToExitPressedOnce = false;
			}
		}, 2000);

		if (findViewById(R.id.now_playing_layout).getVisibility() == View.VISIBLE) {
			findViewById(R.id.now_playing_layout).setVisibility(View.GONE);
			findViewById(R.id.parentLayout).setVisibility(View.VISIBLE);
		}
	}


	@Override
	protected void onResume() {
		super.onResume();
		Log.i("Playing", "onResumed");
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.i("Playing", "onRestart");
	}

	public void customToast(String text) {
		Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
	}

	//reading all music files from sdCard using ArrayList<>
	public ArrayList<File> updateSongList(File root) {
		ArrayList<File> arrayList = new ArrayList<File>();
		File[] files = root.listFiles();  //all files from root directory //file array
		for (File singleFile : files) {
			if (singleFile.isDirectory() && !singleFile.isHidden()) {
				arrayList.addAll(updateSongList(singleFile));
			} else {
				//picking up only .mp3 and .wav format files
				if (singleFile.getName().endsWith(".mp3") || singleFile.getName().endsWith(".wav")) {
					arrayList.add(singleFile);
				}
			}
		}
		return arrayList;
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i("Playing:", "onPause");
		//finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_playing, menu);
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (v.getId() == android.R.id.list) {
			getMenuInflater().inflate(R.menu.menu_song_properties, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
			case R.id.share:
				customToast("Sharing functionality will be implemented soon :P");
				return true;
			case R.id.set_as:
				customToast("SET AS functionality will be implemented soon :P");
				return true;
			case R.id.add_to_playlist:
				customToast("Playlist will be implemented soon :P");
				return true;
			case R.id.add_quick_list:
				customToast("Quick list will be implemented soon :P");
				return true;
			case R.id.deletion: {

				customToast("Deletion will be implemented soon :P");
			}
			return true;
			default:
				return super.onContextItemSelected(item);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_exit:
				this.finish();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.i("Playing ", "onStop");
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		int currentSongPosition = songPositionFromList;
		Log.i("NowPlaying", "onCompletion");
		//Playing next song automatically
		mp.stop();
		mp.reset();
		//if (isDialogAOk == true)
		//updateSongInfoFromDialog(position);
		//Log.i("dialog index:", "" + position);

		if (shuffleFlag && !isRepeatOneOn) {
			songPositionFromList = randomPosition.nextInt((songs.size() - 0) + 0);
		} else if (shuffleFlag && isRepeatOn) {
			songPositionFromList = randomPosition.nextInt((songs.size() - 0) + 0);
		} else if ((!shuffleFlag && isRepeatOn) || (!shuffleFlag && !isRepeatOn) || isRepeatOn) {
			songPositionFromList = (songPositionFromList + 1) % songs.size();
		} else if (isRepeatOneOn) {
			songPositionFromList = currentSongPosition;
		}
		uri = Uri.parse(songs.get(songPositionFromList).toString());
		try {
			mp.setDataSource(getApplicationContext(), uri);
			mp.prepare();
			//Log.i("position ", "" + position);
		} catch (IOException e) {
			e.printStackTrace();
		}
		mp.start();
		updateSongInfo(songPositionFromList);
		//seekBarUpdating.start();
	}

	private class MediaCursorAdapter extends SimpleCursorAdapter {

		public MediaCursorAdapter(Context context, int layout, Cursor c) {
			super(context, layout, c,
					new String[]{MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.TITLE, MediaStore.Audio.AudioColumns.DURATION},
					new int[]{R.id.displayname, R.id.title, R.id.duration});
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TextView title = (TextView) view.findViewById(R.id.title);
			TextView name = (TextView) view.findViewById(R.id.displayname);
			TextView duration = (TextView) view.findViewById(R.id.duration);

			name.setText(cursor.getString(
					cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)));

			title.setText(cursor.getString(
					cursor.getColumnIndex(MediaStore.MediaColumns.TITLE)));

			long durationInMs = Long.parseLong(cursor.getString(
					cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)));

			double durationInMin = ((double) durationInMs / 1000.0) / 60.0;

			durationInMin = new BigDecimal(Double.toString(durationInMin)).setScale(2, BigDecimal.ROUND_UP).doubleValue();

			duration.setText("" + durationInMin);

			view.setTag(cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA)));
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			LayoutInflater inflater = LayoutInflater.from(context);
			View v = inflater.inflate(R.layout.listitem, parent, false);

			bindView(v, context, cursor);

			return v;
		}
	}

	private Runnable UpdateSongTimeT = new Runnable() {
		@Override
		public void run() {
			try {
				startTime = player.getCurrentPosition();
				leftDuration.setText(String.format("%d:%d",
						TimeUnit.MILLISECONDS.toMinutes((long) startTime),
						TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
								TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
										toMinutes((long) startTime)))
				);
				bar.setProgress((int) startTime);
				myHandler.postDelayed(this, 100);
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
	};

	public void updateSongInfo(int thisOne) {
		playPause.setImageResource(R.drawable.btn_pause);
		finalTime = player.getDuration();
		bar.setMax((int) finalTime);
		//bar.setProgress(0);
		//currentSong.setText("" + songs.get(thisOne).getName().replace(".mp3", "").replace(".MP3", "").replace(".wav", "").replace(".WAV", "").replace("_", " "));
		//setting player button
		if (!player.isPlaying()) {
			playPause.setImageResource(R.drawable.btn_play);
		}
		rightDuration.setText(String.format("%d:%d",
				TimeUnit.MILLISECONDS.toMinutes((long) finalTime),
				TimeUnit.MILLISECONDS.toSeconds((long) finalTime) -
						TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) finalTime)))
		);
		bar.setProgress((int) startTime);
		myHandler.postDelayed(UpdateSongTimeT, 100);
		/*if (active) {
			openActivityNotification(getApplicationContext());
		}*/
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (id) {

			case R.id.playPause: {
				try {
					if (player.isPlaying()) {
						player.pause();
						playPause.setImageResource(R.drawable.btn_play);
					} else {
						Log.i("position ", ""+songPositionFromList);
						player.start();
						playPause.setImageResource(R.drawable.btn_pause);
						updateSongInfo(songPositionFromList);
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				}
				break;
			}
			//next button actions
			case R.id.next:
				try {
					player.stop();
					player.reset();
					if (shuffleFlag) {
						songPositionFromList = randomPosition.nextInt((songs.size() - 0) + 0);
					} else {
						songPositionFromList = (songPositionFromList + 1) % songs.size();
					}
					uri = Uri.parse(songs.get(songPositionFromList).toString());
					try {
						player.setDataSource(getApplicationContext(), uri);
						player.prepare();
					} catch (IOException e) {
						e.printStackTrace();
					}
					//Log.i("songPositionFromList ", "" + songPositionFromList);
					player.start();
					updateSongInfo(songPositionFromList);
					playPause.setImageResource(R.drawable.btn_pause);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				}
				break;
			//previous button actions
			case R.id.previous:
				try {
					player.stop();
					player.reset();
					//Log.i("Previous ", "" + songPositionFromList);
					if (songPositionFromList - 1 < 0) {
						if (shuffleFlag) {
							songPositionFromList = randomPosition.nextInt((songsSize - 0) + 0);
						} else {
							songPositionFromList = songsSize - 1;
						}
					} else {
						if (shuffleFlag)
							songPositionFromList = randomPosition.nextInt((songsSize - 0) + 0);
						else
							songPositionFromList--;
					}
					currentFile = songList.getItemAtPosition(songPositionFromList).toString();
					Log.i("PreviousFile ", "" + currentFile);
					//uri = Uri.parse(songList.getItemAtPosition(songPositionFromList).toString());
					//uri = Uri.parse(songs.get(songPositionFromList).toString());
					try {
						//player.setDataSource(getApplicationContext(), uri);
						player.setDataSource(currentFile);
						player.prepare();
					} catch (IOException e) {
						e.printStackTrace();
					}
					playPause.setImageResource(R.drawable.btn_pause);
					//Log.i("songPositionFromList ", "" + songPositionFromList);
					player.start();
					updateSongInfo(songPositionFromList);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				}
				break;
			case R.id.playingSong:{
				if (findViewById(R.id.now_playing_layout).getVisibility() == View.GONE)
					findViewById(R.id.now_playing_layout).setVisibility(View.VISIBLE);
			}
			case R.id.shuffle_all_songs: {
				Toast.makeText(getApplicationContext(), "Not implemented yet :(", Toast.LENGTH_SHORT).show();
			}
		}
	}


}
