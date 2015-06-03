package com.carltondennis.spotifystreamer;

import android.app.Fragment;
import android.content.Intent;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import java.util.HashMap;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.RetrofitError;


/**
 * A placeholder fragment containing a simple view.
 */
public class TracksActivityFragment extends Fragment {

    private static final String TAG = MainActivityFragment.class.getSimpleName();

    public static final String SPOTIFY_ID_KEY = "spotify_id";
    public static final String ARTIST_NAME_KEY = "artist_name";

    private ListView mTracksList;
    private TracksAdapter mTracksAdapter;
    private Toast mToast;

    public TracksActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_tracks, container, false);

        mTracksAdapter = new TracksAdapter(getActivity(), null, 0);
        mTracksList = (ListView) rootView.findViewById(R.id.tracks_list);
        mTracksList.setAdapter(mTracksAdapter);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Intent intent = getActivity().getIntent();
        if (intent != null && intent.getExtras() != null && intent.getExtras().containsKey(SPOTIFY_ID_KEY)) {
            String spotifyId = intent.getExtras().getString(SPOTIFY_ID_KEY);
            String albumName = intent.getExtras().getString(ARTIST_NAME_KEY);

            getActivity().getActionBar().setSubtitle(albumName);

            FetchTop10TracksTask tracksTask = new FetchTop10TracksTask();
            tracksTask.execute(spotifyId);

//            getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        }
    }

    private static final String[] TRACK_COLUMNS = {
            "_id",
            "name",
            "album",
            "album_image_large",
            "album_image_small",
            "preview_url"
    };

    public static final int COL_TRACK_ID = 0;
    public static final int COL_TRACK_NAME = 1;
    public static final int COL_TRACK_ALBUM = 2;
    public static final int COL_TRACK_ALBUM_IMAGE_LARGE = 3;
    public static final int COL_TRACK_ALBUM_IMAGE_SMALL = 4;
    public static final int COL_TRACK_PREVIEW_URL = 5;

    class FetchTop10TracksTask extends AsyncTask<String, Void, MatrixCursor> {
        @Override
        protected MatrixCursor doInBackground(String... params) {

            // we need an spotify id to search for
            if (params.length == 0) {
                return null;
            }

            Log.d(TAG, String.format("Searching for Track: %s", params[0]));

            try {

                SpotifyApi api = new SpotifyApi();
                SpotifyService spotify = api.getService();
                HashMap<String, Object> options = new HashMap<>();
                options.put("country", "US");
                Tracks results = spotify.getArtistTopTrack(params[0], options);
                MatrixCursor cursor = new MatrixCursor(TRACK_COLUMNS);

                int count = results.tracks.size();
                if ( count > 0 ) {
                    for (int i = 0; i < count; i++) {
                        Track track = results.tracks.get(i);
                        String imageUrlLarge = null, imageUrlSmall = null;
                        int imageCount = track.album.images.size();
                        if (imageCount > 0) {
                            for (int j = 0; j < imageCount; j++) {
                                Image image = track.album.images.get(j);
                                if (image.height >= 640) {
                                    imageUrlLarge = image.url;
                                } else if (image.height >= 200) {
                                    imageUrlSmall = image.url;
                                }
                            }
                        }
                        cursor.addRow(new Object[] {i, track.name, track.album.name, imageUrlLarge, imageUrlSmall, track.preview_url});
                    }

                    return cursor;
                }

            } catch (RetrofitError error) {
                Log.d(TAG, error.getMessage());

            }

            return null;
        }

        @Override
        protected void onPostExecute(MatrixCursor result) {
            if (result != null) {
                mTracksAdapter.swapCursor(result);
            } else {
                if (mToast != null) {
                    mToast.cancel();
                }

                mToast = Toast.makeText(getActivity(), R.string.tracks_not_found, Toast.LENGTH_SHORT);
                mToast.show();
            }
        }
    }
}
