package com.github.mrm1st3r.cards.activity;

import java.util.HashMap;
import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mrm1st3r.cards.Cards;
import com.github.mrm1st3r.cards.Constant;
import com.github.mrm1st3r.cards.R;
import com.github.mrm1st3r.cards.lobby.LobbyFragment;
import com.github.mrm1st3r.libdroid.connect.AsynchronousConnection;
import com.github.mrm1st3r.libdroid.connect.OnConnectionChangeHandler;
import com.github.mrm1st3r.libdroid.connect.OnReceivedHandler;
import com.github.mrm1st3r.libdroid.connect.ThreadedConnection;
import com.github.mrm1st3r.libdroid.connect.bluetooth.BluetoothUtil;
import com.github.mrm1st3r.libdroid.connect.bluetooth.ServerThread;
import com.github.mrm1st3r.libdroid.connect.bluetooth.SimpleBluetoothConnection;
import com.github.mrm1st3r.libdroid.util.ResultAction;
import com.github.mrm1st3r.libdroid.widget.HashMapAdapter;

/**
 * This activity will create a new Bluetooth server socket and wait for incoming
 * connections.
 * 
 * @author Lukas 'mrm1st3r' Taake
 * @version 1.0
 */
public class LobbyCreateActivity extends Activity {

	/**
	 * Debug tag.
	 */
	private static final String TAG = LobbyCreateActivity.class.getSimpleName();
	/**
	 * Number of seconds that Bluetooth discoverable will be activated.
	 */
	private static final int LOBBY_CREATE_TIMEOUT = 60;
	/**
	 * Maximum number of remote players that might connect to a lobby.
	 */
	private static final int MAXIMUM_REMOTE_PLAYER_COUNT = 3;
	/**
	 * Background thread that will wait for incoming client connections.
	 */
	private ServerThread mServ = null;
	/**
	 * List of all connected players.
	 */
	private HashMap<SimpleBluetoothConnection, String> mPlayerList =
			new HashMap<SimpleBluetoothConnection, String>();
	/**
	 * Adapter for {@link #mPlayerList}.
	 */
	private HashMapAdapter<SimpleBluetoothConnection, String>
			mPlayerListAdapter;
	/**
	 * Button that will start the actual game when there are any players
	 * connected.
	 */
	private Button mBtnStart = null;
	/**
	 * Local player name.
	 */
	private String mLocalPlayerName;
	/**
	 * The Bluetooth adapters enabling status before the app was started.
	 */
	private boolean mOldBtState = false;

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// go back to start if bluetooth is not supported
		if (!BluetoothUtil.isSupported()) {
			Toast.makeText(this, getString(R.string.bluetooth_not_supported),
					Toast.LENGTH_LONG).show();
			onBackPressed();
		}

		setContentView(R.layout.activity_lobby_create);
		((TextView) findViewById(R.id.txtLobbyName)).setText(BluetoothUtil
				.getDeviceName());

		mOldBtState = BluetoothUtil.isEnabled();
		
		BluetoothUtil.requestEnable(this, new ResultAction() {

			@Override
			public void onSuccess() {

				BluetoothUtil.requestEnableDiscoverable(
						LobbyCreateActivity.this,
						LOBBY_CREATE_TIMEOUT, new ResultAction() {
							@Override
							public void onSuccess() {
								createLobby();
							}

							@Override
							public void onFailure() {
								onBackPressed();
							}
						});
			}

			@Override
			public void onFailure() {
				onBackPressed();
			}
		});
	}

	/**
	 * Initialize missing user interface elements and create a new bluetooth
	 * service for the lobby.
	 */
	private void createLobby() {
		if (mServ != null) {
			// this method should only be called once.
			return;
		}
		((Cards) getApplication()).setEnabled(!mOldBtState);
		// read local player name from preferences
		SharedPreferences pref = getSharedPreferences(Cards.PREF_FILE,
				Context.MODE_PRIVATE);
		mLocalPlayerName = pref.getString(Cards.PREF_PLAYER_NAME, "");
		mPlayerList.put(null, mLocalPlayerName);

		mBtnStart = (Button) findViewById(R.id.btnStart);
		LobbyFragment lobbyFragment = (LobbyFragment) getFragmentManager()
				.findFragmentById(R.id.player_list);

		mPlayerListAdapter =
				new HashMapAdapter<SimpleBluetoothConnection, String>(
				LobbyCreateActivity.this, mPlayerList) {
			@Override
			public View getView(final int pos, final View convertView,
					final ViewGroup parent) {
				TextView rowView;
				if (convertView == null) {
					LayoutInflater inflater = (LayoutInflater) getContext()
							.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					rowView = (TextView) inflater.inflate(
							android.R.layout.simple_list_item_1, parent, false);
				} else {
					rowView = (TextView) convertView;
				}

				rowView.setText(getItem(pos));

				return rowView;
			}

		};
		lobbyFragment.setAdapter(mPlayerListAdapter);

		mServ = new ServerThread(getString(R.string.app_name),
				UUID.fromString(Cards.UUID), new OnConnectionChangeHandler() {
					@Override
					public void onConnect(final ThreadedConnection conn) {
						clientConnected(conn);
					}
				});
		mServ.start();
	}

	/**
	 * Handle an incoming client connection.
	 * 
	 * @param tc
	 *            New incoming client connection
	 */
	private void clientConnected(final ThreadedConnection tc) {
		// new connection for each player
		SimpleBluetoothConnection conn = (SimpleBluetoothConnection) tc;

		// do not accept new connections when maximum number is reached.
		if (mPlayerList.size() == MAXIMUM_REMOTE_PLAYER_COUNT) {
			conn.write("quit");
			conn.close();
			return;
		}
		
		// send player list to new player
		for (String player : mPlayerList.values()) {
			conn.write("join " + player);
		}

		conn.setOnReceivedHandler(new OnReceivedHandler<String>() {
			@Override
			public void onReceived(final AsynchronousConnection<String> ac,
					final String msg) {
				handleIncomingMessage(ac, msg);
			}
		});

		conn.setOnConnectionChangeHandler(new OnConnectionChangeHandler() {
			@Override
			public void onDisconnect(final ThreadedConnection conn) {
				playerLeft(conn);
			}
		});
		// start connection thread
		conn.start();
	}

	/**
	 * Notify other players and update user interface when a player left the
	 * lobby.
	 * 
	 * @param conn
	 *            The left players connection (might already be closed)
	 */
	private void playerLeft(final ThreadedConnection conn) {
		// temporarily save player name for notification
		String leftPlayer = mPlayerList.get(conn);

		mPlayerList.remove(conn);

		// update user interface
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mPlayerListAdapter.notifyDataSetChanged();

				// can't start game with no other players
				if (mPlayerList.size() == 0) {
					mBtnStart.setEnabled(false);
				}
			}
		});
		// send leave note to remaining players
		broadcast("left " + leftPlayer);
	}

	/**
	 * Handle an incoming message.
	 * 
	 * @param ac
	 *            Connection where the message came from
	 * @param msg
	 *            The received message
	 */
	private void handleIncomingMessage(final AsynchronousConnection<String> ac,
			final String msg) {

		SimpleBluetoothConnection conn = (SimpleBluetoothConnection) ac;

		String[] set = msg.split(" ");
		final int namePos = 5;

		if (set[0].equals("join")) {
			// no player name sent
			if (set[1].length() == 0) {
				return;
			}
			mPlayerList.put(conn, msg.substring(namePos));
			// send new player name to other players
			broadcast(msg);

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mBtnStart.setEnabled(true);
					Log.d(TAG, "received: " + msg);
					mPlayerListAdapter.notifyDataSetChanged();
				}
			});

		} else if (set[0].equals("left") && set[1].equals(
				mPlayerList.get(ac))) {
			// verify correct player name on leave

			playerLeft((SimpleBluetoothConnection) ac);
		} else {
			Log.w(TAG, "Illegal message received: " + msg);
		}
	}

	/**
	 * Send a message to all connected clients.
	 * 
	 * @param msg
	 *            Message to send
	 */
	private void broadcast(final String msg) {
		for (SimpleBluetoothConnection c : mPlayerList.keySet()) {
			if (c != null) {
				c.write(msg);
			}
		}
	}

	/**
	 * Close the server thread, notify all connected clients and start the
	 * actual game.
	 * 
	 * @param v
	 *            Calling user interface element
	 */
	public final void start(final View v) {
		((Cards) getApplication()).setConnections(mPlayerList);

		mPlayerList.remove(null);

		// send start command to clients and pause connections
		for (SimpleBluetoothConnection conn : mPlayerList.keySet()) {
			conn.write("start");
			conn.pause();
		}
		// stop listening for new connections.
		mServ.close();

		Intent intent = new Intent(this, HostGameActivity.class);
		intent.putExtra(Constant.EXTRA_LOCAL_NAME, mLocalPlayerName);
		startActivity(intent);
		finish();
	}

	/**
	 * Close the server thread and close all client connections.
	 */
	private void cancelLobby() {
		if (mServ != null) {
			mServ.close();
		}
		mPlayerList.remove(null);
		for (SimpleBluetoothConnection conn : mPlayerList.keySet()) {
			conn.write("quit");
			conn.close();
		}
		mPlayerList.clear();
	}

	/**
	 * Re-enable Bluetooth discoverable mode.
	 * 
	 * @param item
	 *            Calling menu item
	 */
	public final void becomeVisible(final MenuItem item) {
		BluetoothUtil.requestEnableDiscoverable(
				this, LOBBY_CREATE_TIMEOUT, null);
	}

	@Override
	protected final void onActivityResult(final int reqCode,
			final int resultCode, final Intent data) {
		super.onActivityResult(reqCode, resultCode, data);

		BluetoothUtil.onActivityResult(this, reqCode, resultCode, data);
	}

	@Override
	public final void onBackPressed() {
		super.onBackPressed();
		cancelLobby();
	}

	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.lobby_create, menu);
		return true;
	}
}
