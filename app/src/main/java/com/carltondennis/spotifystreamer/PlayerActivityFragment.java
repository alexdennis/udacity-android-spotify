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
import java.util.ArrayList;


/**
 * A placeholder fragment containing a simple view.
 */
public class PlayerActivityFragment extends Fragment {

    private static final String TAG = PlayerActivityFragment.class.getSimpleName();

    public static final String TRACK_KEY = "track";
    public static final String TRACKS_KEY = "tracks";
    public static final String ARTIST_KEY = "artist";
    public static final String TRACK_SEEK_KEY = "track_seek_pos";

    private static int SECOND_IN_MILLISECONDS = 1000;


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
    private ArrayList<SpotifyTrack> mTracks;
    private int mCurrentTrackPosition = -1;

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
                mHandler.postDelayed(this, SECOND_IN_MILLISECONDS);
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

        mButtonPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrevious();
            }
        });
        mButtonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        });
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
                    mMediaPlayer.seekTo(progress);
                    mHandler.postDelayed(mUpdateTimeTask, SECOND_IN_MILLISECONDS);
                }
            }
        });

        return rootView;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(TRACKS_KEY) && savedInstanceState.containsKey(TRACK_KEY) && savedInstanceState.containsKey(TRACK_SEEK_KEY)) {
                mTracks = savedInstanceState.getParcelableArrayList(TRACKS_KEY);
                mCurrentTrackPosition = savedInstanceState.getInt(TRACK_KEY, 0);
                int seekPos = savedInstanceState.getInt(TRACK_SEEK_KEY, 0);
                prepareTrackAndPlay(seekPos);
                return;
            }
        }

        Intent intent = getActivity().getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                if (extras.containsKey(ARTIST_KEY)) {
                    mArtistView.setText(extras.getString(ARTIST_KEY));
                }

                if (extras.containsKey(TRACK_KEY) && extras.containsKey(TRACKS_KEY)) {
                    mTracks = extras.getParcelableArrayList(TRACKS_KEY);
                    mCurrentTrackPosition = extras.getInt(TRACK_KEY);
                    prepareTrackAndPlay();
                }


            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mTracks != null) {
            outState.putParcelableArrayList(TRACKS_KEY, mTracks);
        }
        outState.putInt(TRACK_KEY, mCurrentTrackPosition);
        if (mMediaPlayer != null) {
            outState.putInt(TRACK_SEEK_KEY, mMediaPlayer.getCurrentPosition());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMediaPlayer.release();
        mMediaPlayer = null;
    }


    private void togglePlayback()
    {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mButtonPlayPause.setImageResource(android.R.drawable.ic_media_play);
                mMediaPlayer.pause();
                mHandler.removeCallbacks(mUpdateTimeTask);
            } else {
                mMediaPlayer.start();
                mButtonPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                mHandler.postDelayed(mUpdateTimeTask, SECOND_IN_MILLISECONDS);
            }
        }

    }

    private void playNext()
    {
        mCurrentTrackPosition++;
        // play next track unless we are at the end of the list.
        if (mCurrentTrackPosition >= mTracks.size()) {
            mCurrentTrackPosition = mTracks.size() - 1;
        } else {
            prepareTrackAndPlay();
        }
    }

    private void playPrevious()
    {
        // Restart track if its been playing for more than a second.
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            int currentSeekPos = mMediaPlayer.getCurrentPosition();
            if (currentSeekPos >= SECOND_IN_MILLISECONDS) {
                mMediaPlayer.reset();
                mMediaPlayer.start();
                return;
            }
        }

        mCurrentTrackPosition--;
        if (mCurrentTrackPosition < 0) {
            mCurrentTrackPosition = 0;
        }
        prepareTrackAndPlay();
    }

    private String milliSecondsToTime(int milliSeconds)
    {
        int minutes = (milliSeconds / SECOND_IN_MILLISECONDS) / 60;
        int seconds = (milliSeconds / SECOND_IN_MILLISECONDS) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void toastFailedPlayback()
    {
        if (mToast != null) {
            mToast.cancel();
        }

        mToast = Toast.makeText(getActivity(), R.string.failed_playback, Toast.LENGTH_SHORT);
        mToast.show();
    }

    private void prepareTrackAndPlay()
    {
        prepareTrackAndPlay(0);
    }

    private void prepareTrackAndPlay(int seekPos)
    {
        if (mCurrentTrackPosition < 0 || mCurrentTrackPosition >= mTracks.size()) {
            toastFailedPlayback();
            return;
        }

        SpotifyTrack track = mTracks.get(mCurrentTrackPosition);
        if (track == null) {
            toastFailedPlayback();
            return;
        }

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


        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mTrackProgressView.setText(milliSecondsToTime(mp.getCurrentPosition()));
                playNext();
            }
        });

        try {
            if (track.previewURL != null && track.previewURL.length() > 0) {
                Log.d(TAG, "Preparing source: " + track.previewURL);
                mMediaPlayer.setDataSource(track.previewURL);
                mMediaPlayer.prepare(); // might take long! (for buffering, etc)
                if (seekPos > 0) {
                    mMediaPlayer.seekTo(seekPos);
                }
                togglePlayback();

                int duration = mMediaPlayer.getDuration();
                mTrackDurationView.setText(milliSecondsToTime(duration));
                mTrackSeekBar.setMax(duration);
            } else {
                toastFailedPlayback();
            }

        } catch (IOException ioex) {
            Log.d(TAG, ioex.getMessage());
            toastFailedPlayback();
        }
    }

}
