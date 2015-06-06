package com.carltondennis.spotifystreamer;

import android.app.Activity;
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
            ArrayList<SpotifyTrack> tracks = getIntent().getParcelableArrayListExtra(PlayerActivityFragment.TRACKS_KEY);
            int trackIndex = getIntent().getIntExtra(PlayerActivityFragment.TRACK_KEY, 0);
            String artistName = getIntent().getStringExtra(PlayerActivityFragment.ARTIST_KEY);

            Bundle args = new Bundle();
            args.putParcelableArrayList(PlayerActivityFragment.TRACKS_KEY, tracks);
            args.putInt(PlayerActivityFragment.TRACK_KEY, trackIndex);
            args.putString(PlayerActivityFragment.ARTIST_KEY, artistName);

            PlayerActivityFragment f = new PlayerActivityFragment();
            f.setArguments(args);

            getFragmentManager().beginTransaction()
                    .replace(R.id.player_container, f)
                    .commit();
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
