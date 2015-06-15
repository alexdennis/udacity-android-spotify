package com.carltondennis.spotifystreamer.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.carltondennis.spotifystreamer.PlaybackService;
import com.carltondennis.spotifystreamer.R;

/**
 * Created by alex on 6/14/15.
 */
public abstract class BaseActivity extends Activity {

    private static final String TAG = BaseActivity.class.getSimpleName();

    public static final String ACTION_TOKEN_UPDATE = "com.carltondennis.spotifystreamer.intent.action.TOKEN_UPDATE";
    public static final String SESSION_TOKEN_KEY = "session_token";

    private Menu mMenu;
    private MediaSession.Token mToken;
    private PlaybackState mLastPlaybackState;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_TOKEN_UPDATE)) {
                Bundle extras = intent.getExtras();
                mToken = extras.getParcelable(SESSION_TOKEN_KEY);
                connectToSession(mToken);
            }
        }
    };

    private MediaController.Callback mCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            Log.d(TAG, "onPlaybackstate changed " + state);
            updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
        }
    };

    private void connectToSession(MediaSession.Token token) {
        MediaController controller = new MediaController(this, token);
        controller.registerCallback(mCallback);
        PlaybackState state = controller.getPlaybackState();
        updatePlaybackState(state);
    }

    public void showNowPlaying() {
        if (mMenu != null) {
            MenuItem nowPlaying = mMenu.findItem(R.id.action_now_playing);
            nowPlaying.setVisible(true);
        }
    }

    public void hideNowPlaying() {
        if (mMenu != null) {
            MenuItem nowPlaying = mMenu.findItem(R.id.action_now_playing);
            nowPlaying.setVisible(false);
        }
    }

    protected void updatePlaybackState(PlaybackState state) {
        mLastPlaybackState = state;

        if (state == null) {
            return;
        }

        switch (state.getState()) {
            case PlaybackState.STATE_PLAYING:
                showNowPlaying();
                break;
            default:
                hideNowPlaying();
                break;
        }
    }

    public void showPlayer(boolean isTwoPane) {
        if (mToken != null) {
            if (isTwoPane) {
                PlayerActivityFragment f = PlayerActivityFragment.newInstance(mToken);
                f.show(getFragmentManager(), "dialog");
            } else {
                Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
                intent.putExtra(PlayerActivityFragment.SESSION_TOKEN_KEY, mToken);
                startActivity(intent);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_now_playing) {
            showPlayer(false);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);

        if (mLastPlaybackState != null && mLastPlaybackState.getState() == PlaybackState.STATE_PLAYING) {
            showNowPlaying();
        }
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter(ACTION_TOKEN_UPDATE);
        registerReceiver(mReceiver, intentFilter);

        // Notify service to send us an token update
        startService(new Intent(this, PlaybackService.class));
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onRestoreInstanceState (Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.containsKey(SESSION_TOKEN_KEY)) {
            mToken = savedInstanceState.getParcelable(SESSION_TOKEN_KEY);
            connectToSession(mToken);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SESSION_TOKEN_KEY, mToken);
    }
}
