package com.github.mrm1st3r.cards;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.github.mrm1st3r.cards.game.ui.BotGameActivity;
import com.github.mrm1st3r.cards.lobby.LobbyCreateActivity;
import com.github.mrm1st3r.cards.lobby.LobbyJoinActivity;

/**
 * This is the main activity that is started when the app gets started. It
 * contains a player name input field and buttons to reach all of the apps
 * functions.
 * 
 * @author Lukas 'mrm1st3r' Taake
 * @version 1.0
 */
public class MainActivity extends Activity {

	/**
	 * Debug tag.
	 */
	@SuppressWarnings("unused")
	private static final String TAG = MainActivity.class.getSimpleName();
	/**
	 * Preferences for player name.
	 */
	private SharedPreferences mPrefs;
	/**
	 * Input field for player name.
	 */
	private EditText mInputPlayerName;

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// read currently saved player name
		mPrefs = getSharedPreferences(Cards.PREF_FILE, Context.MODE_PRIVATE);
		// use null as default value to be able to immediately save
		// default value to prevent empty user name.
		String name = mPrefs.getString(Cards.PREF_PLAYER_NAME, null);

		if (name == null) {
			name = BluetoothAdapter.getDefaultAdapter().getName();
			changePlayerName(name);
		}

		mInputPlayerName = (EditText) findViewById(R.id.input_player_name);
		mInputPlayerName.setText(name);
		mInputPlayerName.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(final Editable input) {
			}

			@Override
			public void beforeTextChanged(final CharSequence arg0,
					final int arg1, final int arg2, final int arg3) {
			}

			@Override
			public void onTextChanged(final CharSequence name, final int arg1,
					final int arg2, final int arg3) {
				changePlayerName(name.toString());
			}
		});
	}

	/**
	 * Write a new player name to preferences.
	 * 
	 * @param newName
	 *            Player name to wrote
	 */
	private void changePlayerName(final String newName) {
		SharedPreferences.Editor edit = mPrefs.edit();

		edit.putString(Cards.PREF_PLAYER_NAME, newName);
		edit.commit();
	}

	/**
	 * React to a user input and start a new activity.
	 * 
	 * @param v
	 *            The button that was pressed
	 */
	public final void onButtonPressed(final View v) {
		Class<? extends Activity> activity = null;
		int id = v.getId();

		if (id == R.id.btn_new_game) {
			activity = LobbyCreateActivity.class;
			
		} else if (id == R.id.btn_join_game) {
			activity = LobbyJoinActivity.class;
			
		} else if (id == R.id.btn_bot_game) {
			activity = BotGameActivity.class;
		} else if (id == R.id.btn_rules) {
			activity = RulesActivity.class;
			
		} else {
			return;
		}
		Intent intent = new Intent(this, activity);
		intent.putExtra(Constant.EXTRA_LOCAL_NAME, mInputPlayerName.getText());
		startActivity(intent);
	}
}