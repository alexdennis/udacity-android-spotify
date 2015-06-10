package com.carltondennis.spotifystreamer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.Rating;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

public class PlaybackService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener {

    public static final String TAG = PlaybackService.class.getSimpleName();

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_REWIND = "action_rewind";
    public static final String ACTION_FAST_FORWARD = "action_fast_foward";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";
    public static final String ACTION_SEEK_TO = "action_seek_to";

    private MediaPlayer mMediaPlayer;
    private ArrayList<SpotifyTrack> mTracks;
    private int mTracksQueuePosition;
    private int mCurrentPosition;
    private int mState;
    private MediaController mController;
    private MediaSession mSession;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
        } else if (action.equalsIgnoreCase(ACTION_FAST_FORWARD)) {
            mController.getTransportControls().fastForward();
        } else if (action.equalsIgnoreCase(ACTION_REWIND)) {
            mController.getTransportControls().rewind();
        } else if (action.equalsIgnoreCase(ACTION_PREVIOUS)) {
            mController.getTransportControls().skipToPrevious();
        } else if (action.equalsIgnoreCase(ACTION_NEXT)) {
            mController.getTransportControls().skipToNext();
        } else if (action.equalsIgnoreCase(ACTION_STOP)) {
            mController.getTransportControls().stop();
        } else if (action.equalsIgnoreCase(ACTION_SEEK_TO)) {
            mController.getTransportControls().seekTo(intent.getLongExtra("seekPos", 0));
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

            initMediaSessions();
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

        Intent intent = new Intent(getApplicationContext(), PlaybackService.class);
        intent.setAction(ACTION_STOP);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(track.name)
                .setContentText(track.albumName)
                .setDeleteIntent(pendingIntent)
                .setStyle(style);

//        try {
//            Bitmap aBitmap = Picasso.with(getApplicationContext()).load(track.imageLargeURL).get();
//            builder.setLargeIcon(aBitmap);
//        } catch (IOException ioex) {
//            Log.d(TAG, "Did not get bitmap for notification");
//        }

        builder.addAction(generateAction(android.R.drawable.ic_media_previous, "Previous", ACTION_PREVIOUS));
//        builder.addAction(generateAction(android.R.drawable.ic_media_rew, "Rewind", ACTION_REWIND));
        builder.addAction(action);
//        builder.addAction(generateAction(android.R.drawable.ic_media_ff, "Fast Foward", ACTION_FAST_FORWARD));
        builder.addAction(generateAction(android.R.drawable.ic_media_next, "Next", ACTION_NEXT));
        style.setShowActionsInCompactView(0, 1, 2, 3, 4);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onPrepared(MediaPlayer player) {

        player.start();
        if (player.isPlaying()) {
            mState = PlaybackState.STATE_PLAYING;
            updatePlaybackState(null);
        }
    }

    /**
     * Called when there's an error playing media. When this happens, the media
     * player goes to the Error state. We warn the user about the error and
     * reset the media player.
     *
     * @see MediaPlayer.OnErrorListener
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "Media player error: what=" + what + ", extra=" + extra);
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

    private void createMediaPlayerIfNeeded() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();

            // Make sure the media player will acquire a wake-lock while
            // playing. If we don't do that, the CPU might go to sleep while the
            // song is playing, causing playback to stop.
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            // we want the media player to notify us when it's ready preparing,
            // and when it's done playing:
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
        } else {
            mMediaPlayer.reset();
        }
    }

    private void play() {
        if (mTracksQueuePosition < 0 || mTracksQueuePosition >= mTracks.size()) {
            Log.d(TAG, String.format("Invalid q pos: %d", mTracksQueuePosition));
            return;
        }

        if (mMediaPlayer != null && mState == PlaybackState.STATE_PAUSED) {
            mMediaPlayer.start();
            if (mMediaPlayer.isPlaying()) {
                mState = PlaybackState.STATE_PLAYING;
                updatePlaybackState(null);
            }
            return;
        }

        SpotifyTrack track = mTracks.get(mTracksQueuePosition);

        try {
            if (track.isPlayable()) {
                Log.d(TAG, "Preparing source: " + track.previewURL);
                createMediaPlayerIfNeeded();
                mMediaPlayer.setDataSource(track.previewURL);
                mMediaPlayer.prepareAsync();
                mState = PlaybackState.STATE_BUFFERING;
            }
        } catch (IOException ioex) {
            Log.d(TAG, ioex.getMessage());
        }
    }

    private void pause() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mState = PlaybackState.STATE_PAUSED;
                updatePlaybackState(null);
            }
        }

    }

    private void skipToNext() {
        mTracksQueuePosition++;
        // play next track unless we are at the end of the list.
        if (mTracksQueuePosition >= mTracks.size()) {
            mTracksQueuePosition = mTracks.size() - 1;
        } else {
            play();
        }
    }

    private void skipToPrevious() {
        // Restart track if its been playing for more than a 5 seconds.
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            int currentSeekPos = mMediaPlayer.getCurrentPosition();
            if (currentSeekPos >= (Utility.SECOND_IN_MILLISECONDS * 5)) {
                mMediaPlayer.seekTo(0);
                mMediaPlayer.start();
                return;
            }
        }

        mTracksQueuePosition--;
        if (mTracksQueuePosition < 0) {
            mTracksQueuePosition = 0;
        }
        play();
    }

    private void stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mState = PlaybackState.STATE_STOPPED;
            updatePlaybackState(null);
        }
    }

    private void seekTo(int position) {
        Log.d(TAG, "seekTo called with " + position);

        if (mMediaPlayer == null) {
            // If we do not have a current media player, simply update the current position
            mCurrentPosition = position;
        } else {
            if (mMediaPlayer.isPlaying()) {
                mState = PlaybackState.STATE_BUFFERING;
            }
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

    /**
     * Update the current media player state, optionally showing an error message.
     *
     * @param error if not null, error message to present to the user.
     */
    private void updatePlaybackState(String error) {
        Log.d(TAG, "updatePlaybackState, playback state=" + mState);
        long position = PlaybackState.PLAYBACK_POSITION_UNKNOWN;
        long duration = 0;
        if (mMediaPlayer != null) {
            position = mMediaPlayer.getCurrentPosition();
            duration = mMediaPlayer.getDuration();
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

//        mSession.setPlaybackState(stateBuilder.build());

        Intent i = new Intent(PlayerActivityFragment.PlaybackUpdateReceiver.CUSTOM_INTENT);
        Bundle extras = new Bundle();
        extras.putParcelable(PlayerActivityFragment.PlaybackUpdateReceiver.PLAYBACK_KEY, stateBuilder.build());
        extras.putLong(PlayerActivityFragment.PlaybackUpdateReceiver.DURATION_KEY, duration);
        i.putExtras(extras);
        sendBroadcast(i);

//        if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_PAUSED) {
//            mMediaNotificationManager.startNotification();
//        }
    }

    private void initMediaSessions() {
        mSession = new MediaSession(getApplicationContext(), "simple player session");
        mController = new MediaController(getApplicationContext(), mSession.getSessionToken());
        mSession.setCallback(new MediaSessionCallback());
        mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        updatePlaybackState(null);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mSession.release();
        mMediaPlayer = null;
        return super.onUnbind(intent);
    }

    private final class MediaSessionCallback extends MediaSession.Callback {
        @Override
        public void onPlay() {
            super.onPlay();
            Log.e("MediaPlayerService", "onPlay");
            play();
            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
        }

        @Override
        public void onPause() {
            super.onPause();
            Log.e("MediaPlayerService", "onPause");
            pause();
            buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY));
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            Log.e("MediaPlayerService", "onSkipToNext");
            skipToNext();
            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));

        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            Log.e("MediaPlayerService", "onSkipToPrevious");
            skipToPrevious();
            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
        }

        @Override
        public void onFastForward() {
            super.onFastForward();
            Log.e("MediaPlayerService", "onFastForward");
            //Manipulate current media here
        }

        @Override
        public void onRewind() {
            super.onRewind();
            Log.e("MediaPlayerService", "onRewind");
            //Manipulate current media here
        }

        @Override
        public void onStop() {
            super.onStop();
            Log.e("MediaPlayerService", "onStop");
            //Stop media player here
            stop();

            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(1);
            Intent intent = new Intent(getApplicationContext(), PlaybackService.class);
            stopService(intent);
        }

        @Override
        public void onSeekTo(long pos) {

            super.onSeekTo(pos);
            seekTo((int) pos);
        }

        @Override
        public void onSetRating(Rating rating) {
            super.onSetRating(rating);
        }

    }
}
