package com.carltondennis.spotifystreamer;

import android.app.Fragment;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import retrofit.RetrofitError;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private static final String TAG = MainActivityFragment.class.getCanonicalName();

    private ArtistsAdapter mArtistsAdapter;
    private ListView mArtistsList;
    private EditText mArtistSearchBox;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_main, container, false);

        mArtistsAdapter = new ArtistsAdapter(getActivity(), null, 0);
        mArtistsList = (ListView) rootView.findViewById(R.id.artists_list);
        mArtistsList.setAdapter(mArtistsAdapter);
        mArtistSearchBox = (EditText) rootView.findViewById(R.id.search_artists);
        mArtistSearchBox.addTextChangedListener(new ArtistSearchTextWatcher());

        return rootView;
    }

    class ArtistSearchTextWatcher implements TextWatcher {

        @Override
        public void afterTextChanged(Editable s) {}

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String text = mArtistSearchBox.getText().toString().trim();
            if (text.length() > 2) {
                Log.d(TAG, text);
                FetchArtistsTask artistsTask = new FetchArtistsTask();
                artistsTask.execute(text);
            }
        }
    }

    private static final String[] ARTIST_COLUMNS = {
            "_id",
            "image",
            "name",
            "artist_id"
    };

    class FetchArtistsTask extends AsyncTask<String, Void, MatrixCursor> {
        @Override
        protected MatrixCursor doInBackground(String... params) {

            // we need an artist to search for
            if (params.length == 0) {
                return null;
            }

            Log.d(TAG, String.format("Searching for Artist: %s", params[0]));

            try {

                SpotifyApi api = new SpotifyApi();
                SpotifyService spotify = api.getService();
                ArtistsPager results = spotify.searchArtists(params[0]);
                MatrixCursor cursor = new MatrixCursor(ARTIST_COLUMNS);

                int count = results.artists.items.size();
                if ( count > 0 ) {
                    for (int i = 0; i < count; i++) {
                        Artist artist = results.artists.items.get(i);
                        String imageUrl = null;
                        if (artist.images.size() > 0) {
                            imageUrl = artist.images.get(0).url;
                        }
                        cursor.addRow(new Object[] {i, imageUrl, artist.name, artist.id});
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
                mArtistsAdapter.swapCursor(result);
            }
        }
    }
}
