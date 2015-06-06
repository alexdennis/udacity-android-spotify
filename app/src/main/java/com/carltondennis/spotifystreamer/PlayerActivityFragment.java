package com.carltondennis.spotifystreamer;

import android.app.Fragment;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Callback;
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
    private ImageButton mButtonPrevious;
    private ImageButton mButtonNext;
    private ImageButton mButtonPlayPause;

    private MediaPlayer mMediaPlayer;
    private Handler mHandler = new Handler();
    private Toast mToast;

    public PlayerActivityFragment() {
    }

    /**
     * Background Runnable thread
     * */
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                int currentDuration = mMediaPlayer.getCurrentPosition();
                mTrackProgressView.setText(milliSecondsToTime(currentDuration));
                mTrackSeekBar.setProgress(currentDuration);

                // Running this thread after 1000 milliseconds
                mHandler.postDelayed(this, 1000);
            }
        }
    };

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
        mButtonNext = (ImageButton) rootView.findViewById(R.id.player_btn_next);
        mButtonPlayPause = (ImageButton) rootView.findViewById(R.id.player_btn_play);
        mButtonPrevious = (ImageButton) rootView.findViewById(R.id.player_btn_previous);

        mButtonPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlayback();
            }
        });
        mTrackSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mMediaPlayer != null && fromUser) {
                    mHandler.removeCallbacks(mUpdateTimeTask);
                    mMediaPlayer.seekTo(progress * 1000);
                    mHandler.postDelayed(mUpdateTimeTask, 1000);
                }
            }
        });

        return rootView;

    }


    private void togglePlayback()
    {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mButtonPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                mMediaPlayer.pause();
                mHandler.removeCallbacks(mUpdateTimeTask);
            } else {
                mMediaPlayer.start();
                mButtonPlayPause.setImageResource(android.R.drawable.ic_media_play);
                mHandler.postDelayed(mUpdateTimeTask, 1000);
            }
        }

    }

    private void playNext()
    {

    }

    private void playPrevious()
    {

    }

    private String milliSecondsToTime(int milliSeconds)
    {
        int minutes = (milliSeconds / 1000) / 60;
        int seconds = (milliSeconds / 1000) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void prepareTrackAndPlay(SpotifyTrack track)
    {
        mAlbumView.setText(track.albumName);
        mTrackView.setText(track.name);
        Picasso.with(getActivity())
                .load(track.imageLargeURL)
                .transform(PaletteTransformation.instance())
                .into(mAlbumImageView, new Callback.EmptyCallback() {
                    @Override
                    public void onSuccess() {
                        Bitmap bitmap = ((BitmapDrawable) mAlbumImageView.getDrawable()).getBitmap(); // Ew!
                        Palette palette = PaletteTransformation.getPalette(bitmap);

                        int foreground = palette.getDarkVibrantColor(0xFFFFFF);
                        int background = palette.getLightVibrantColor(0xCCCCCC);

                        getView().setBackgroundColor(background);
                        mButtonPlayPause.setColorFilter(foreground);
                        mButtonPlayPause.setBackgroundColor(background);
                        mButtonNext.setColorFilter(foreground);
                        mButtonNext.setBackgroundColor(background);
                        mButtonPrevious.setColorFilter(foreground);
                        mButtonPrevious.setBackgroundColor(background);
                        mTrackSeekBar.setProgressTintList(ColorStateList.valueOf(foreground));
                        mTrackSeekBar.setThumbTintList(ColorStateList.valueOf(foreground));
                    }
                });


        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mMediaPlayer.setDataSource(track.previewURL);
            mMediaPlayer.prepare(); // might take long! (for buffering, etc)
            togglePlayback();

            int duration = mMediaPlayer.getDuration();
            mTrackDurationView.setText(milliSecondsToTime(duration));
            mTrackSeekBar.setMax(duration);

        } catch (IOException ioex) {
            Log.d(TAG, ioex.getMessage());

            if (mToast != null) {
                mToast.cancel();
            }

            mToast = Toast.makeText(getActivity(), R.string.failed_playback, Toast.LENGTH_SHORT);
            mToast.show();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Intent intent = getActivity().getIntent();
        if (intent != null && intent.getExtras() != null && intent.getExtras().containsKey(TRACK_KEY)) {
            SpotifyTrack track = intent.getExtras().getParcelable(TRACK_KEY);
            String artist = intent.getExtras().getString(ARTIST_KEY);
            mArtistView.setText(artist);

            prepareTrackAndPlay(track);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMediaPlayer.release();
        mMediaPlayer = null;
    }
}
