package com.carltondennis.spotifystreamer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;


public class MainActivity extends Activity implements MainActivityFragment.Callback, TracksActivityFragment.Callback {

    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (findViewById(R.id.fragment_tracks) != null) {
            mTwoPane = true;
        } else {
            mTwoPane = false;
        }
    }

    public boolean isTwoPane()
    {
        return mTwoPane;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onArtistSelected(SpotifyArtist artist) {

        Bundle extras = new Bundle();
        String spotifyId = artist.spotifyId;

        if (spotifyId == null || spotifyId.length() == 0) {
            return;
        }

        extras.putString(TracksActivityFragment.SPOTIFY_ID_KEY, spotifyId);
        extras.putString(TracksActivityFragment.ARTIST_NAME_KEY, artist.name);

        if (!isTwoPane()) {
            Intent intent = new Intent(this, TracksActivity.class)
                    .putExtras(extras);
            startActivity(intent);
        } else {
            TracksActivityFragment fragment = new TracksActivityFragment();
            fragment.setArguments(extras);
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_tracks, fragment)
                    .commit();
        }
    }

    public void onTrackSelected(ArrayList<SpotifyTrack> tracks, int trackIndex, String artistName) {
        PlayerActivityFragment fragment = PlayerActivityFragment.newInstance(tracks, trackIndex, artistName);
        fragment.show(getFragmentManager(), "dialog");
    }
}
