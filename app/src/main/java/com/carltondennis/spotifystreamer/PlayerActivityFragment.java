package com.carltondennis.spotifystreamer;

import android.app.Fragment;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.IOException;


/**
 * A placeholder fragment containing a simple view.
 */
public class PlayerActivityFragment extends Fragment {

    private static final String TAG = PlayerActivityFragment.class.getSimpleName();

    public static final String TRACK_KEY = "track";
    public static final String ARTIST_KEY = "artist";


    private TextView mAlbumView;
    private TextView mArtistView;
    private TextView mTrackView;
    private ImageView mAlbumImageView;
    private TextView mTrackProgressView;
    private TextView mTrackDurationView;
    private SeekBar mTrackSeekBar;

    private MediaPlayer mMediaPlayer;
    private Toast mToast;

    public PlayerActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_player, container, false);

        mAlbumView = (TextView) rootView.findViewById(R.id.player_album);
        mArtistView = (TextView) rootView.findViewById(R.id.player_artist);
        mTrackView = (TextView) rootView.findViewById(R.id.player_track_name);
        mAlbumImageView = (ImageView) rootView.findViewById(R.id.player_album_image);
        mTrackProgressView = (TextView) rootView.findViewById(R.id.player_track_progress);
        mTrackDurationView = (TextView) rootView.findViewById(R.id.player_track_duration);
        mTrackSeekBar = (SeekBar) rootView.findViewById(R.id.player_track_seek_bar);

        return rootView;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Intent intent = getActivity().getIntent();
        if (intent != null && intent.getExtras() != null && intent.getExtras().containsKey(TRACK_KEY)) {
            SpotifyTrack track = intent.getExtras().getParcelable(TRACK_KEY);
            String artist = intent.getExtras().getString(ARTIST_KEY);

            mAlbumView.setText(track.albumName);
            mArtistView.setText(artist);
            mTrackView.setText(track.name);
            Picasso.with(getActivity()).load(track.imageLargeURL).into(mAlbumImageView);

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            try {
                mMediaPlayer.setDataSource(track.previewURL);
                mMediaPlayer.prepare(); // might take long! (for buffering, etc)
                mMediaPlayer.start();

                int duration = mMediaPlayer.getDuration();
                Log.d(TAG, String.format("Duration = %d", duration));
                int minutes = (duration / 1000) / 60;
                int seconds = (duration / 1000) % 60;
                Log.d(TAG, String.format("Duration = %d:%d", minutes, seconds));

                mTrackDurationView.setText(String.format("%d:%d", minutes, seconds));

            } catch (IOException ioex) {
                Log.d(TAG, ioex.getMessage());

                if (mToast != null) {
                    mToast.cancel();
                }

                mToast = Toast.makeText(getActivity(), R.string.failed_playback, Toast.LENGTH_SHORT);
                mToast.show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mMediaPlayer.release();
        mMediaPlayer = null;
    }
}
