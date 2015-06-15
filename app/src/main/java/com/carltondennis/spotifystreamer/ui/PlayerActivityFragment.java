package com.carltondennis.spotifystreamer.ui;

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.ShareActionProvider;
import android.widget.TextView;

import com.carltondennis.spotifystreamer.PaletteTransformation;
import com.carltondennis.spotifystreamer.PlaybackService;
import com.carltondennis.spotifystreamer.R;
import com.carltondennis.spotifystreamer.Utility;
import com.carltondennis.spotifystreamer.data.SpotifyTrack;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;


/**
 * A placeholder fragment containing a simple view.
 */
public class PlayerActivityFragment extends DialogFragment {

    private static final String TAG = PlayerActivityFragment.class.getSimpleName();

    public static final String ACTION_TOKEN_UPDATE = "com.carltondennis.spotifystreamer.intent.action.PLAYBACK_UPDATE";

    public static final String TRACK_KEY = "track";
    public static final String TRACKS_KEY = "tracks";
    public static final String SESSION_TOKEN_KEY = "session_token";

    private static final long PROGRESS_UPDATE_INTERNAL = 1000;
    private static final long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;

    private TextView mAlbumView;
    private TextView mArtistView;
    private TextView mTrackView;
    private TextView mLine4View;
    private ImageView mAlbumImageView;
    private TextView mTrackProgressView;
    private TextView mTrackDurationView;
    private SeekBar mTrackSeekBar;
    private ImageButton mButtonPrevious;
    private ImageButton mButtonNext;
    private ImageButton mButtonPlayPause;
    private ProgressBar mLoading;


    private Handler mHandler = new Handler();
    private int mState = PlaybackState.STATE_NONE;
    private PlaybackState mLastPlaybackState;
    private MediaSession.Token mToken;
    private MediaController mController;
    private MenuItem mShareMenuItem;
    private boolean mInTabletMode;

    public PlayerActivityFragment() {
        setHasOptionsMenu(true);
    }

    public static PlayerActivityFragment newInstance(ArrayList<SpotifyTrack> tracks, int trackIndex) {
        Bundle args = new Bundle();
        args.putParcelableArrayList(PlayerActivityFragment.TRACKS_KEY, tracks);
        args.putInt(PlayerActivityFragment.TRACK_KEY, trackIndex);

        PlayerActivityFragment f = new PlayerActivityFragment();
        f.setArguments(args);
        return f;
    }

    public static PlayerActivityFragment newInstance(MediaSession.Token token) {
        Bundle args = new Bundle();
        args.putParcelable(PlayerActivityFragment.SESSION_TOKEN_KEY, token);
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


    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MainActivity.ACTION_TOKEN_UPDATE)) {
                Bundle extras = intent.getExtras();
                mToken = extras.getParcelable(MainActivity.SESSION_TOKEN_KEY);
                connectToSession(mToken);
            }
        }
    };

    private MediaController.Callback mCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            Log.d(TAG, "onPlaybackstate changed " + state);
            updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            if (metadata != null) {
                updateMediaDescription(metadata);
                updateDuration(metadata);
            }
        }
    };

    private void connectToSession(MediaSession.Token token) {
        mController = new MediaController(getActivity(), token);
        mController.registerCallback(mCallback);
        PlaybackState state = mController.getPlaybackState();
        updatePlaybackState(state);
        MediaMetadata metadata = mController.getMetadata();
        if (metadata != null) {
            updateMediaDescription(metadata);
            updateDuration(metadata);
        }
        updateProgress();
        if (state != null && (state.getState() == PlaybackState.STATE_PLAYING ||
                state.getState() == PlaybackState.STATE_BUFFERING)) {
            scheduleSeekbarUpdate();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_player, container, false);

        mAlbumView = (TextView) rootView.findViewById(R.id.player_album);
        mArtistView = (TextView) rootView.findViewById(R.id.player_artist);
        mTrackView = (TextView) rootView.findViewById(R.id.player_track_name);
        mAlbumImageView = (ImageView) rootView.findViewById(R.id.player_album_image);
        mLine4View = (TextView) rootView.findViewById(R.id.player_line4);
        mTrackProgressView = (TextView) rootView.findViewById(R.id.player_track_progress);
        mTrackDurationView = (TextView) rootView.findViewById(R.id.player_track_duration);
        mTrackSeekBar = (SeekBar) rootView.findViewById(R.id.player_track_seek_bar);
        mButtonNext = (ImageButton) rootView.findViewById(R.id.player_btn_next);
        mButtonPlayPause = (ImageButton) rootView.findViewById(R.id.player_btn_play);
        mButtonPrevious = (ImageButton) rootView.findViewById(R.id.player_btn_previous);
        mLoading = (ProgressBar) rootView.findViewById(R.id.player_loading);

        mButtonPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mController.getTransportControls().skipToPrevious();
            }
        });
        mButtonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mController.getTransportControls().skipToNext();
            }
        });
        mButtonPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mController == null) {
                    return;
                }

                switch (mState) {
                    case PlaybackState.STATE_PLAYING: // fall through
                    case PlaybackState.STATE_BUFFERING:
                        mController.getTransportControls().pause();
                        mButtonPlayPause.setImageResource(android.R.drawable.ic_media_play);
                        stopSeekbarUpdate();
                        break;
                    case PlaybackState.STATE_PAUSED:
                    case PlaybackState.STATE_STOPPED:
                        mController.getTransportControls().play();
                        mButtonPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                        scheduleSeekbarUpdate();
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

                mController.getTransportControls().seekTo(seekBar.getProgress());
                scheduleSeekbarUpdate();
            }
        });

        return rootView;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.containsKey(SESSION_TOKEN_KEY)) {
            mToken = savedInstanceState.getParcelable(SESSION_TOKEN_KEY);
            connectToSession(mToken);
            return;
        }

        Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey(SESSION_TOKEN_KEY)) {
                mToken = args.getParcelable(SESSION_TOKEN_KEY);
                connectToSession(mToken);
            } else if (args.containsKey(TRACK_KEY) && args.containsKey(TRACKS_KEY)) {
                // First time this fragment is run since there is no state, so we
                // have to pass the new track list and queue position to the service
                Intent intent = new Intent(getActivity(), PlaybackService.class);
                intent.setAction(PlaybackService.ACTION_PLAY);
                intent.putExtras(args);
                getActivity().startService(intent);
            }
        }

        mInTabletMode = (getActivity() instanceof MainActivity);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_player_fragment, menu);


        // Set up ShareActionProvider's default share intent
        mShareMenuItem = menu.findItem(R.id.action_share);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter(MainActivity.ACTION_TOKEN_UPDATE);
        getActivity().registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SESSION_TOKEN_KEY, mToken);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSeekbarUpdate();
        mExecutorService.shutdown();
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

    private void createShareIntent(String spotifyUrl) {

        if (mShareMenuItem == null) {
            Log.d(TAG, "Share Menu Item is null?");
            return;
        }

        if (mInTabletMode) {
            Log.d(TAG, "No share menu item for tablet mode");
            return;
        }

        ShareActionProvider shareActionProvider = (ShareActionProvider) mShareMenuItem.getActionProvider();
        if (shareActionProvider == null) {
            Log.d(TAG, "Share Action Provider is null?");
        } else if (spotifyUrl == null) {
            Log.d(TAG, "Spotify Url String is null?");
        } else {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, spotifyUrl);
            shareActionProvider.setShareIntent(intent);
            mShareMenuItem.setVisible(true);
        }
    }

    private void updateMediaDescription(MediaMetadata mediaMetadata) {
        if (mediaMetadata == null) {
            return;
        }
        Log.d(TAG, "updateMediaDescription called ");

        String album = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
        String title = mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        String artist = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        String art = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI);
        String previewUrl = mediaMetadata.getString(PlaybackService.CUSTOM_METADATA_TRACK_URL);

        mAlbumView.setText(album);
        mTrackView.setText(title);
        mArtistView.setText(artist);

        // This will be shared via sharing intent
        createShareIntent(previewUrl);

        Picasso.with(getActivity())
                .load(art)
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
                        mLoading.setProgressTintList(ColorStateList.valueOf(foreground));
                        mLoading.setBackgroundTintList(ColorStateList.valueOf(foreground));
                    }
                });
    }

    private void updatePlaybackState(PlaybackState state) {
        if (state == null) {
            return;
        }
        mLastPlaybackState = state;
        mState = mLastPlaybackState.getState();
        mLine4View.setText("");

        switch (state.getState()) {
            case PlaybackState.STATE_PLAYING:
                mLoading.setVisibility(INVISIBLE);
                mButtonPlayPause.setVisibility(VISIBLE);
                mButtonPlayPause.setImageResource(android.R.drawable.ic_media_pause);
//                mControllers.setVisibility(VISIBLE);
                scheduleSeekbarUpdate();
                break;
            case PlaybackState.STATE_PAUSED:
//                mControllers.setVisibility(VISIBLE);
                mLoading.setVisibility(INVISIBLE);
                mButtonPlayPause.setVisibility(VISIBLE);
                mButtonPlayPause.setImageResource(android.R.drawable.ic_media_play);
                stopSeekbarUpdate();
                break;
            case PlaybackState.STATE_NONE:
                mLoading.setVisibility(INVISIBLE);
                mButtonPlayPause.setVisibility(VISIBLE);
                mButtonPlayPause.setImageResource(android.R.drawable.ic_media_play);
                stopSeekbarUpdate();
                break;
            case PlaybackState.STATE_BUFFERING:
                mButtonPlayPause.setVisibility(INVISIBLE);
                mLoading.setVisibility(VISIBLE);
                mLine4View.setText(R.string.loading);
                stopSeekbarUpdate();
                break;
            case PlaybackState.STATE_STOPPED:
                if (mInTabletMode) {
                    dismiss();
                } else {
                    getActivity().finish();
                }
            default:
                Log.d(TAG, "Unhandled state " + state.getState());
        }
    }

    private void updateDuration(MediaMetadata metadata) {
        if (metadata == null) {
            return;
        }
        Log.d(TAG, "updateDuration called ");
        int duration = (int) metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        mTrackDurationView.setText(Utility.fromMillisecs(duration));
        mTrackSeekBar.setMax(duration);
    }

}
