package com.carltondennis.spotifystreamer;

import android.app.Activity;
import android.content.Intent;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;


public class PlayerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if (intent != null) {
                if (intent.hasExtra(PlayerActivityFragment.TRACKS_KEY) && intent.hasExtra(PlayerActivityFragment.TRACK_KEY)) {
                    ArrayList<SpotifyTrack> tracks = intent.getParcelableArrayListExtra(PlayerActivityFragment.TRACKS_KEY);
                    int trackIndex = intent.getIntExtra(PlayerActivityFragment.TRACK_KEY, 0);
                    PlayerActivityFragment f = PlayerActivityFragment.newInstance(tracks, trackIndex);
                    getFragmentManager().beginTransaction()
                            .replace(R.id.player_container, f)
                            .commit();
                }

                if (intent.hasExtra(PlayerActivityFragment.SESSION_TOKEN_KEY)) {
                    MediaSession.Token token = intent.getParcelableExtra(PlayerActivityFragment.SESSION_TOKEN_KEY);
                    PlayerActivityFragment f = PlayerActivityFragment.newInstance(token);
                    getFragmentManager().beginTransaction()
                            .replace(R.id.player_container, f)
                            .commit();
                }
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
