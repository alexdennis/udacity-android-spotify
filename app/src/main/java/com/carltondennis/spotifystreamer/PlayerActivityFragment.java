package com.carltondennis.spotifystreamer;

import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * A placeholder fragment containing a simple view.
 */
public class PlayerActivityFragment extends DialogFragment {

    private static final String TAG = PlayerActivityFragment.class.getSimpleName();

    public static final String TRACK_KEY = "track";
    public static final String TRACKS_KEY = "tracks";
    public static final String ARTIST_KEY = "artist";
    public static final String STATE_KEY  = "state";

    private static final long PROGRESS_UPDATE_INTERNAL = 1000;
    private static final long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;

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


    private Handler mHandler = new Handler();
    private ArrayList<SpotifyTrack> mTracks;
    private long mCurrentTrackQueueId = -1;
    private int mState = PlaybackState.STATE_NONE;
    private PlaybackState mLastPlaybackState;
    private MediaSession.Token mToken;
    private MediaController mController;

    public PlayerActivityFragment() {
    }

    public static PlayerActivityFragment newInstance(ArrayList<SpotifyTrack> tracks, int trackIndex, String artistName) {
        Bundle args = new Bundle();
        args.putParcelableArrayList(PlayerActivityFragment.TRACKS_KEY, tracks);
        args.putInt(PlayerActivityFragment.TRACK_KEY, trackIndex);
        args.putString(PlayerActivityFragment.ARTIST_KEY, artistName);

        PlayerActivityFragment f = new PlayerActivityFragment();
        f.setArguments(args);
        return f;
    }

    /**
     * Background Runnable thread
     */
    private Runnable mUpdateTimeTask = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    private final ScheduledExecutorService mExecutorService =
            Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> mScheduleFuture;

    private PlaybackUpdateReceiver mPlaybackUpdateReceiver = new PlaybackUpdateReceiver();
    public class PlaybackUpdateReceiver extends BroadcastReceiver {

        public static final String SESSION_UPDATE = "com.carltondennis.spotifystreamer.intent.action.SESSION_UPDATE";
        public static final String CUSTOM_INTENT = "com.carltondennis.spotifystreamer.intent.action.PLAYBACK_UPDATE";
        public static final String PLAYBACK_KEY = "playbackUpate";
        public static final String DURATION_KEY = "duration";
        public static final String SESSION_KEY = "session";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PlaybackUpdateReceiver.CUSTOM_INTENT)) {
                Bundle extras = intent.getExtras();
                mLastPlaybackState = extras.getParcelable(PLAYBACK_KEY);
                long duration = extras.getLong(DURATION_KEY);

                mState = mLastPlaybackState.getState();
                updatePlayer(mLastPlaybackState, duration);
                updateProgress();
            } else if (intent.getAction().equals(PlaybackUpdateReceiver.SESSION_UPDATE)) {
                Bundle extras = intent.getExtras();
                mToken = extras.getParcelable(SESSION_KEY);
                connectToSession(mToken);
            }
        }
    }

    private MediaController.Callback mCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            Log.d(TAG, "onPlaybackstate changed " + state);
//            updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            if (metadata != null) {
//                updateMediaDescription(metadata.getDescription());
//                updateDuration(metadata);
            }
        }
    };

    private void connectToSession(MediaSession.Token token) {
        mController = new MediaController(getActivity(), token);
        if (mController.getMetadata() == null) {
//            finish();
            return;
        }
        mController.registerCallback(mCallback);
        PlaybackState state = mController.getPlaybackState();
//        updatePlaybackState(state);
//        MediaMetadata metadata = mediaController.getMetadata();
//        if (metadata != null) {
//            updateMediaDescription(metadata.getDescription());
//            updateDuration(metadata);
//        }
        updateProgress();
        if (state != null && (state.getState() == PlaybackState.STATE_PLAYING ||
                state.getState() == PlaybackState.STATE_BUFFERING)) {
            scheduleSeekbarUpdate();
        }
    }

    private void updateProgress() {
        if (mLastPlaybackState == null) {
            return;
        }
        long currentPosition = mLastPlaybackState.getPosition();
        if (mLastPlaybackState.getState() != PlaybackState.STATE_PAUSED) {
            // Calculate the elapsed time between the last position update and now and unless
            // paused, we can assume (delta * speed) + current position is approximately the
            // latest position. This ensure that we do not repeatedly call the getPlaybackState()
            // on MediaController.
            long timeDelta = SystemClock.elapsedRealtime() -
                    mLastPlaybackState.getLastPositionUpdateTime();
            currentPosition += (int) timeDelta * mLastPlaybackState.getPlaybackSpeed();
        }
        mTrackSeekBar.setProgress((int) currentPosition);
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
        mButtonNext = (ImageButton) rootView.findViewById(R.id.player_btn_next);
        mButtonPlayPause = (ImageButton) rootView.findViewById(R.id.player_btn_play);
        mButtonPrevious = (ImageButton) rootView.findViewById(R.id.player_btn_previous);

        mButtonPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(getActivity(), PlaybackService.class);
//                intent.setAction(PlaybackService.ACTION_PREVIOUS);
//                getActivity().startService(intent);

                mController.getTransportControls().skipToPrevious();
            }
        });
        mButtonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(getActivity(), PlaybackService.class);
//                intent.setAction(PlaybackService.ACTION_NEXT);
//                getActivity().startService(intent);
                mController.getTransportControls().skipToNext();
            }
        });
        mButtonPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                if (mController == null) {
                    return;
                }

                switch (mState) {
                    case PlaybackState.STATE_PLAYING: // fall through
                    case PlaybackState.STATE_BUFFERING:
//                        intent = new Intent(getActivity(), PlaybackService.class);
//                        intent.setAction(PlaybackService.ACTION_PAUSE);
//                        getActivity().startService(intent);
//                        stopSeekbarUpdate();
                        mController.getTransportControls().pause();
                        mButtonPlayPause.setImageResource(android.R.drawable.ic_media_play);
                        break;
                    case PlaybackState.STATE_PAUSED:
                    case PlaybackState.STATE_STOPPED:
                        mController.getTransportControls().play();
//                        intent = new Intent(getActivity(), PlaybackService.class);
//                        intent.setAction(PlaybackService.ACTION_PLAY);
//                        getActivity().startService(intent);
//                        scheduleSeekbarUpdate();
                        mButtonPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                        break;
                    default:
                        Log.d(TAG, String.format("onClick with state = %d", mState));
                }
            }
        });
        mTrackSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mTrackProgressView.setText(Utility.fromMillisecs(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopSeekbarUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                Intent intent = new Intent(getActivity(), PlaybackService.class);
                intent.setAction(PlaybackService.ACTION_SEEK_TO);
                Bundle extras = new Bundle();
                extras.putInt(PlaybackService.SEEK_POS_KEY, seekBar.getProgress());
                intent.putExtras(extras);
                getActivity().startService(intent);
                scheduleSeekbarUpdate();
            }
        });

        return rootView;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey(ARTIST_KEY)) {
                mArtistView.setText(args.getString(ARTIST_KEY));
            }

            if (args.containsKey(TRACK_KEY) && args.containsKey(TRACKS_KEY)) {
                mTracks = args.getParcelableArrayList(TRACKS_KEY);

                Intent intent = new Intent(getActivity(), PlaybackService.class);


                if (savedInstanceState != null && savedInstanceState.containsKey(STATE_KEY)) {
                    mState = savedInstanceState.getInt(STATE_KEY);
                    intent.setAction(PlaybackService.ACTION_UPDATE_STATE);
                } else {
                    // First time this fragment is run since there is no state, so we
                    // have to pass the new track list and queue position to the service
                    intent.setAction(PlaybackService.ACTION_PLAY);
                    intent.putExtras(args);
                }

                getActivity().startService(intent);
                scheduleSeekbarUpdate();
            }
        }
    }

    private void scheduleSeekbarUpdate() {
        stopSeekbarUpdate();
        if (!mExecutorService.isShutdown()) {
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            mHandler.post(mUpdateTimeTask);
                        }
                    }, PROGRESS_UPDATE_INITIAL_INTERVAL,
                    PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS);
        }
    }

    private void stopSeekbarUpdate() {
        if (mScheduleFuture != null) {
            mScheduleFuture.cancel(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(PlaybackUpdateReceiver.CUSTOM_INTENT);
        intentFilter.addAction(PlaybackUpdateReceiver.SESSION_UPDATE);
        getActivity().registerReceiver(mPlaybackUpdateReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mPlaybackUpdateReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_KEY, mState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSeekbarUpdate();
        mExecutorService.shutdown();
    }

    private void updatePlayer(PlaybackState state, long duration) {
        long queueId = state.getActiveQueueItemId();

        if (queueId < 0 || queueId >= mTracks.size()) {
            return;
        }

        if (mCurrentTrackQueueId != queueId) {
            // Media changed
            mCurrentTrackQueueId = queueId;

            SpotifyTrack track = mTracks.get((int) mCurrentTrackQueueId);

            mAlbumView.setText(track.albumName);
            mTrackView.setText(track.name);
            mTrackDurationView.setText(Utility.fromMillisecs((int) duration));
            mTrackSeekBar.setMax((int) duration);
            Picasso.with(getActivity())
                    .load(track.imageLargeURL)
                    .transform(PaletteTransformation.instance())
                    .into(mAlbumImageView, new Callback.EmptyCallback() {
                        @Override
                        public void onSuccess() {
                            Bitmap bitmap = ((BitmapDrawable) mAlbumImageView.getDrawable()).getBitmap(); // Ew!
                            Palette palette = PaletteTransformation.getPalette(bitmap);

                            int foreground = palette.getDarkVibrantColor(0x000000);

                            mButtonPlayPause.setColorFilter(foreground);
                            mButtonNext.setColorFilter(foreground);
                            mButtonPrevious.setColorFilter(foreground);
                            mTrackSeekBar.setProgressTintList(ColorStateList.valueOf(foreground));
                            mTrackSeekBar.setThumbTintList(ColorStateList.valueOf(foreground));
                        }
                    });
        }



    }

}
