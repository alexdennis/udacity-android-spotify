package com.carltondennis.spotifystreamer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.wearable.media.MediaControlConstants;
import android.util.Log;
import android.net.wifi.WifiManager.WifiLock;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.util.ArrayList;

public class PlaybackService extends Service {

    public static final String TAG = PlaybackService.class.getSimpleName();

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";

    private static final int NOTIFICATION_ID = 411;

    private MediaPlayer mMediaPlayer;
    private MediaSession mSession;
    private MediaController mController;

    private ArrayList<SpotifyTrack> mTracks;
    private int mTracksQueuePosition;
    private int mCurrentPosition;
    private int mState;
    private NotificationManager mNotificationManager;
    private WifiLock mWifiLock;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        mTracks = new ArrayList<>();

        // Start a new MediaSession
        mSession = new MediaSession(this, "PlaybackService");
        mSession.setCallback(new MediaSessionCallback());
        mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        Bundle sessionExtras = new Bundle();
        sessionExtras.putBoolean(MediaControlConstants.EXTRA_RESERVE_SLOT_SKIP_TO_PREVIOUS, true);
        sessionExtras.putBoolean(MediaControlConstants.EXTRA_RESERVE_SLOT_SKIP_TO_NEXT, true);
        sessionExtras.putBoolean(MediaControlConstants.EXTRA_BACKGROUND_COLOR_FROM_THEME, true);
        mSession.setExtras(sessionExtras);

        mController = new MediaController(getApplicationContext(), mSession.getSessionToken());
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "my_lock");

    }

    @Override
    public void onDestroy() {
        stop();
        if (mSession != null) {
            mSession.release();
            mSession = null;
        }

        super.onDestroy();
    }

    private void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null)
            return;

        setupQueueFromIntent(intent);

        String action = intent.getAction();

        if (action.equalsIgnoreCase(ACTION_PLAY)) {
            mController.getTransportControls().play();
        } else if (action.equalsIgnoreCase(ACTION_PAUSE)) {
            mController.getTransportControls().pause();
        } else if (action.equalsIgnoreCase(ACTION_PREVIOUS)) {
            mController.getTransportControls().skipToPrevious();
        } else if (action.equalsIgnoreCase(ACTION_NEXT)) {
            mController.getTransportControls().skipToNext();
        } else if (action.equalsIgnoreCase(ACTION_STOP)) {
            mController.getTransportControls().stop();
        }
    }

    private void setupQueueFromIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Log.d(TAG, "Setting up queue for PlaybackService");

            if (extras.containsKey(PlayerActivityFragment.TRACK_KEY) && extras.containsKey(PlayerActivityFragment.TRACKS_KEY)) {
                mTracks = extras.getParcelableArrayList(PlayerActivityFragment.TRACKS_KEY);
                mTracksQueuePosition = extras.getInt(PlayerActivityFragment.TRACK_KEY);
            }
        }
    }

    private Notification.Action generateAction(int icon, String title, String intentAction) {
        Intent intent = new Intent(getApplicationContext(), PlaybackService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        return new Notification.Action.Builder(icon, title, pendingIntent).build();
    }

    private void buildNotification(Notification.Action action) {
        Notification.MediaStyle style = new Notification.MediaStyle();

        if (mTracks == null || mTracksQueuePosition < 0 || mTracksQueuePosition > mTracks.size()) {
            return;
        }
        SpotifyTrack track = mTracks.get(mTracksQueuePosition);

        final Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle(track.name)
                .setContentText(track.artistName)
                .setContentIntent(createContentIntent())
                .setShowWhen(false)
                .setStyle(style);

        builder.addAction(generateAction(android.R.drawable.ic_media_previous, "Previous", ACTION_PREVIOUS));
        builder.addAction(action);
        builder.addAction(generateAction(android.R.drawable.ic_media_next, "Next", ACTION_NEXT));
        style.setShowActionsInCompactView(1);
        style.setMediaSession(mSession.getSessionToken());

        Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);

        Picasso.with(getApplicationContext())
                .load(track.imageLargeURL)
                .into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        // cache is now warmed up
                        builder.setLargeIcon(bitmap);
                        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                    }
                });

    }

    private void relaxResources(boolean releaseMediaPlayer) {
        Log.d(TAG, "relaxResources. releaseMediaPlayer=" + releaseMediaPlayer);

        stopForeground(true);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }

    private PendingIntent createContentIntent() {
        Intent openUI = new Intent(getApplicationContext(), MainActivity.class);
        openUI.putExtra(PlayerActivityFragment.SESSION_TOKEN_KEY, mSession.getSessionToken());
        return PendingIntent.getActivity(getApplicationContext(), 1, openUI,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        sendSessionToken();
        return super.onStartCommand(intent, flags, startId);
    }

    private void createMediaPlayerIfNeeded() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();

            // Make sure the media player will acquire a wake-lock while
            // playing. If we don't do that, the CPU might go to sleep while the
            // song is playing, causing playback to stop.
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            // we want the media player to notify us when it's ready preparing,
            // and when it's done playing:
            MediaPlayerCallbacks callbacks = new MediaPlayerCallbacks();
            mMediaPlayer.setOnPreparedListener(callbacks);
            mMediaPlayer.setOnErrorListener(callbacks);
            mMediaPlayer.setOnCompletionListener(callbacks);
            mMediaPlayer.setOnSeekCompleteListener(callbacks);
        } else {
            mMediaPlayer.reset();
        }
    }

    private void play()
    {
        play(false);
    }

    private void play(boolean isSkipOrPrevious) {
        if (mTracksQueuePosition < 0 || mTracksQueuePosition >= mTracks.size()) {
            Log.d(TAG, String.format("Invalid q pos: %d", mTracksQueuePosition));
            return;
        }

        if (mMediaPlayer != null && !isSkipOrPrevious && mState == PlaybackState.STATE_PAUSED) {
            mMediaPlayer.start();
            if (mMediaPlayer.isPlaying()) {
                mState = PlaybackState.STATE_PLAYING;
                updatePlaybackState(null);
            }
            return;
        }

        SpotifyTrack track = mTracks.get(mTracksQueuePosition);

        relaxResources(false);

        try {
            if (track.isPlayable()) {
                Log.d(TAG, "Preparing source: " + track.previewURL);
                createMediaPlayerIfNeeded();

                mState = PlaybackState.STATE_BUFFERING;

                mMediaPlayer.setDataSource(track.previewURL);
                mMediaPlayer.prepareAsync();

                mWifiLock.acquire();

                updatePlaybackState(null);
            }
        } catch (IOException ioex) {
            Log.d(TAG, ioex.getMessage());
        }
    }

    private void pause() {
        if (mState == PlaybackState.STATE_PLAYING) {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                updatePlaybackState(null);
            }

            relaxResources(false);
        }

        mState = PlaybackState.STATE_PAUSED;
        updatePlaybackState(null);
    }

    private void skipToNext() {
        mTracksQueuePosition++;
        // play next track unless we are at the end of the list.
        if (mTracksQueuePosition >= mTracks.size()) {
            mTracksQueuePosition = mTracks.size() - 1;
        } else {
            play(true);
        }
    }

    private void skipToPrevious() {
        // Restart track if its been playing for more than a 5 seconds.
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            int currentSeekPos = mMediaPlayer.getCurrentPosition();
            if (currentSeekPos >= (Utility.SECOND_IN_MILLISECONDS * 5)) {
                mMediaPlayer.seekTo(0);
                mMediaPlayer.start();
                updatePlaybackState(null);
                return;
            }
        }

        mTracksQueuePosition--;
        if (mTracksQueuePosition < 0) {
            mTracksQueuePosition = 0;
        }
        play(true);
    }

    private void stop() {
        mState = PlaybackState.STATE_STOPPED;
        updatePlaybackState(null);
        relaxResources(true);
        stopSelf();
    }

    private void seekTo(int position) {
        Log.d(TAG, "seekTo called with " + position);

        if (mMediaPlayer == null) {
            // If we do not have a current media player, simply update the current position
            mCurrentPosition = position;
        } else {
//            if (mMediaPlayer.isPlaying()) {
//                mState = PlaybackState.STATE_BUFFERING;
//            }
            mMediaPlayer.seekTo(position);
            updatePlaybackState(null);
        }
    }

    private long getAvailableActions() {
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_FROM_MEDIA_ID |
                PlaybackState.ACTION_PLAY_FROM_SEARCH;
        if (mTracks == null || mTracks.isEmpty()) {
            return actions;
        }
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            actions |= PlaybackState.ACTION_PAUSE;
        }
        if (mTracksQueuePosition > 0) {
            actions |= PlaybackState.ACTION_SKIP_TO_PREVIOUS;
        }
        if (mTracksQueuePosition < mTracks.size() - 1) {
            actions |= PlaybackState.ACTION_SKIP_TO_NEXT;
        }
        return actions;
    }

    private void sendSessionToken() {
        Intent i = new Intent(PlayerActivityFragment.ACTION_TOKEN_UPDATE);
        Bundle extras = new Bundle();
        extras.putParcelable(PlayerActivityFragment.SESSION_TOKEN_KEY, mSession.getSessionToken());
        i.putExtras(extras);
        sendBroadcast(i);
    }

    /**
     * Update the current media player state, optionally showing an error message.
     *
     * @param error if not null, error message to present to the user.
     */
    private void updatePlaybackState(String error) {
        Log.d(TAG, "updatePlaybackState, playback state=" + mState);
        long position = PlaybackState.PLAYBACK_POSITION_UNKNOWN;
        if (mMediaPlayer != null) {
            position = mMediaPlayer.getCurrentPosition();
        }

        PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
                .setActions(getAvailableActions());

        int state = mState;

        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error);
            state = PlaybackState.STATE_ERROR;
        }
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        // Set the activeQueueItemId if the current index is valid.
        if (mTracks != null && mTracksQueuePosition >= 0 && mTracksQueuePosition < mTracks.size()) {
            stateBuilder.setActiveQueueItemId(mTracksQueuePosition);
        }

        mSession.setPlaybackState(stateBuilder.build());

        if (state == PlaybackState.STATE_PLAYING) {
            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
        } else if (state == PlaybackState.STATE_PAUSED) {
            buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY));
        }
    }

    private void updateMetadata()
    {
        if (mTracksQueuePosition < 0 || mTracksQueuePosition >= mTracks.size()) {
            Log.d(TAG, String.format("Invalid q pos: %d", mTracksQueuePosition));
            return;
        }

        int duration = 0;
        if (mMediaPlayer != null) {
            duration = mMediaPlayer.getDuration();
        }

        SpotifyTrack track = mTracks.get(mTracksQueuePosition);
        String id = String.valueOf(track.name.hashCode());

        MediaMetadata metadata = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, id)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, track.albumName)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artistName)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
                .putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, track.imageLargeURL)
                .putString(MediaMetadata.METADATA_KEY_TITLE, track.name)
                .build();

        Log.d(TAG, "Updating metadata for MusicID= " + id);
        mSession.setMetadata(metadata);
    }

    private final class MediaPlayerCallbacks implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
            MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener {

        @Override
        public void onPrepared(MediaPlayer player) {

            player.start();
            if (player.isPlaying()) {
                mState = PlaybackState.STATE_PLAYING;
                updateMetadata();
                updatePlaybackState(null);
            }
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.e(TAG, "Media player error: what=" + what + ", extra=" + extra);
            updatePlaybackState("Media player error: what=" + what + ", extra=" + extra);
            return true; // true indicates we handled the error
        }

        @Override
        public void onCompletion(MediaPlayer mp) {

            skipToNext();
        }

        @Override
        public void onSeekComplete(MediaPlayer mp) {
            Log.d(TAG, "onSeekComplete from MediaPlayer:" + mp.getCurrentPosition());
            mCurrentPosition = mp.getCurrentPosition();
            if (mState == PlaybackState.STATE_BUFFERING) {
                mMediaPlayer.start();
                mState = PlaybackState.STATE_PLAYING;
            }
        }
    }

    private final class MediaSessionCallback extends MediaSession.Callback {
        @Override
        public void onPlay() {
            super.onPlay();
            Log.d(TAG, "play");
            play();
        }

        @Override
        public void onSeekTo(long position) {
            Log.d(TAG, "onSeekTo:" + position);
            seekTo((int) position);
        }

        @Override
        public void onPause() {
            super.onPause();
            Log.d(TAG, "pause. current state=" + mState);
            pause();
        }

        @Override
        public void onStop() {
            super.onStop();
            Log.d(TAG, "onStop");
            stop();
            Intent intent = new Intent(getApplicationContext(), PlaybackService.class);
            stopService(intent);
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            Log.e(TAG, "onSkipToNext");
            skipToNext();
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            Log.d(TAG, "onSkipToPrevious");
            skipToPrevious();
        }
    }

}
