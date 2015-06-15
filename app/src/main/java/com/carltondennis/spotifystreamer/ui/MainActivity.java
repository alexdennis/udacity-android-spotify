package com.carltondennis.spotifystreamer.ui;

import android.content.Intent;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.view.MenuItem;

import com.carltondennis.spotifystreamer.R;
import com.carltondennis.spotifystreamer.data.SpotifyArtist;
import com.carltondennis.spotifystreamer.data.SpotifyTrack;

import java.util.ArrayList;


public class MainActivity extends BaseActivity implements MainActivityFragment.Callback,
        TracksActivityFragment.Callback {

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

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if (intent != null && intent.hasExtra(PlayerActivityFragment.SESSION_TOKEN_KEY)) {
                showPlayer(mTwoPane);
                MediaSession.Token token = intent.getParcelableExtra(PlayerActivityFragment.SESSION_TOKEN_KEY);
                if (mTwoPane) {
                    PlayerActivityFragment f = PlayerActivityFragment.newInstance(token);
                    f.show(getFragmentManager(), "dialog");
                } else {
                    Intent playerIntent = new Intent(this, PlayerActivity.class);
                    playerIntent.putExtra(PlayerActivityFragment.SESSION_TOKEN_KEY, token);
                    startActivity(playerIntent);
                }
            }
        }
    }

    public boolean isTwoPane()
    {
        return mTwoPane;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_now_playing) {
            showPlayer(mTwoPane);
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

    public void onTrackSelected(ArrayList<SpotifyTrack> tracks, int trackIndex) {
        PlayerActivityFragment fragment = PlayerActivityFragment.newInstance(tracks, trackIndex);
        fragment.show(getFragmentManager(), "dialog");
    }
}
