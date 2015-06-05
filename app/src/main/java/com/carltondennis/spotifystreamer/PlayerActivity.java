package com.carltondennis.spotifystreamer;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class PlayerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        if (savedInstanceState == null) {
            SpotifyTrack track = getIntent().getParcelableExtra(PlayerActivityFragment.TRACK_KEY);
            String artistName = getIntent().getStringExtra(PlayerActivityFragment.ARTIST_KEY);

            Bundle args = new Bundle();
            args.putParcelable(PlayerActivityFragment.TRACK_KEY, track);
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
