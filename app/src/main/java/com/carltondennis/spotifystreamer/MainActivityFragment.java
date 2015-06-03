package com.carltondennis.spotifystreamer;

import android.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;
import retrofit.RetrofitError;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private static final String TAG = MainActivityFragment.class.getSimpleName();

    private ArtistsAdapter mArtistsAdapter;
    private ListView mArtistsList;
    private EditText mArtistSearchBox;
    private Toast mToast;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_main, container, false);

        mArtistsAdapter = new ArtistsAdapter(getActivity(), null, 0);
        mArtistsList = (ListView) rootView.findViewById(R.id.artists_list);
        mArtistsList.setAdapter(mArtistsAdapter);
        mArtistsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                ArtistsAdapter adapter = (ArtistsAdapter) adapterView.getAdapter();
                Cursor cursor = adapter.getCursor();

                if (null != cursor && cursor.moveToPosition(position)) {
//                    ((Callback) getActivity()).onItemSelected(cursor.getString(COL_WEATHER_DATE));
//                    mPosition = position;
                    Bundle extras = new Bundle();
                    extras.putString(TracksActivityFragment.SPOTIFY_ID_KEY, cursor.getString(COL_ARTIST_SPOTIFY_ID));
                    extras.putString(TracksActivityFragment.ARTIST_NAME_KEY, cursor.getString(COL_ARTIST_NAME));
                    Intent intent = new Intent(getActivity(), TracksActivity.class)
                            .putExtras(extras);
                    startActivity(intent);
                }
            }
        });

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
            "spotify_id"
    };

    public static final int COL_ARTIST_ID = 0;
    public static final int COL_ARTIST_IMAGE = 1;
    public static final int COL_ARTIST_NAME = 2;
    public static final int COL_ARTIST_SPOTIFY_ID = 3;

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
                            int size = 0;
                            for (int j = 0; j < artist.images.size(); j++) {
                                Image image = artist.images.get(j);
                                if (size == 0 || image.height < size) {
                                    size = image.height;
                                }

                                if (image.height >= 200 && image.height <= size) {
                                    imageUrl = image.url;
                                }
                            }
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
            } else {
                if (mToast != null) {
                    mToast.cancel();
                }

                mToast = Toast.makeText(getActivity(), R.string.artists_not_found, Toast.LENGTH_SHORT);
                mToast.show();
            }
        }
    }
}
