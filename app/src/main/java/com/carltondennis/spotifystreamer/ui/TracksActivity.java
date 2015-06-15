package com.carltondennis.spotifystreamer.ui;

import android.content.Intent;
import android.os.Bundle;

import com.carltondennis.spotifystreamer.R;
import com.carltondennis.spotifystreamer.data.SpotifyTrack;

import java.util.ArrayList;


public class TracksActivity extends BaseActivity implements TracksActivityFragment.Callback {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracks);

        if (savedInstanceState == null) {
            String spotifyId = getIntent().getStringExtra(TracksActivityFragment.SPOTIFY_ID_KEY);
            String artistName = getIntent().getStringExtra(TracksActivityFragment.ARTIST_NAME_KEY);
            TracksActivityFragment f = TracksActivityFragment.newInstance(spotifyId, artistName);

            getFragmentManager().beginTransaction()
                    .replace(R.id.tracks_detail_container, f)
                    .commit();
        }
    }

    public void onTrackSelected(ArrayList<SpotifyTrack> tracks, int trackIndex) {
        Bundle extras = new Bundle();
        extras.putParcelableArrayList(PlayerActivityFragment.TRACKS_KEY, tracks);
        extras.putInt(PlayerActivityFragment.TRACK_KEY, trackIndex);
        Intent intent = new Intent(this, PlayerActivity.class)
                .putExtras(extras);
        startActivity(intent);
    }
}
