package com.carltondennis.spotifystreamer.ui;

import android.app.Fragment;
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

import com.carltondennis.spotifystreamer.R;
import com.carltondennis.spotifystreamer.data.SpotifyArtist;

import java.util.ArrayList;

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
    private static final String POSITION_KEY = "pos";

    private ArtistsAdapter mArtistsAdapter;
    private ListView mArtistsList;
    private EditText mArtistSearchBox;
    private int mPosition;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_main, container, false);

        mArtistsAdapter = new ArtistsAdapter(getActivity(), R.layout.list_item_artist, new ArrayList<SpotifyArtist>());
        mArtistsList = (ListView) rootView.findViewById(R.id.artists_list);
        mArtistsList.setAdapter(mArtistsAdapter);
        mArtistsList.setEmptyView(rootView.findViewById(R.id.artists_not_found));
        mArtistsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                ArtistsAdapter adapter = (ArtistsAdapter) adapterView.getAdapter();
                SpotifyArtist artist = adapter.getItem(position);

                if (artist != null) {
                    ((Callback) getActivity()).onArtistSelected(artist);
                    mPosition = position;
                }
            }
        });

        mArtistSearchBox = (EditText) rootView.findViewById(R.id.search_artists);
        mArtistSearchBox.addTextChangedListener(new ArtistSearchTextWatcher());

        if (savedInstanceState != null && savedInstanceState.containsKey(POSITION_KEY)) {
            mPosition = savedInstanceState.getInt(POSITION_KEY);
        }

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(POSITION_KEY, mPosition);
        }

        super.onSaveInstanceState(outState);
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

    public interface Callback {
        /**
         * Callback for when an item has been selected.
         */
        void onArtistSelected(SpotifyArtist artist);
    }

    class FetchArtistsTask extends AsyncTask<String, Void, ArrayList<SpotifyArtist>> {
        @Override
        protected ArrayList<SpotifyArtist> doInBackground(String... params) {

            // we need an artist to search for
            if (params.length == 0) {
                return null;
            }

            Log.d(TAG, String.format("Searching for Artist: %s", params[0]));

            try {

                SpotifyApi api = new SpotifyApi();
                SpotifyService spotify = api.getService();
                ArtistsPager results = spotify.searchArtists(params[0]);
                ArrayList<SpotifyArtist> displayArtists = new ArrayList<>();

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
                        displayArtists.add(new SpotifyArtist(artist.name, imageUrl, artist.id));
                    }

                    return displayArtists;
                }

            } catch (RetrofitError error) {
              Log.d(TAG, error.getMessage());

            }

            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<SpotifyArtist> result) {
            mArtistsAdapter.clear();
            if (result != null) {
                mArtistsAdapter.addAll(result);
            } else {
                mArtistsAdapter.notifyDataSetChanged();
            }
        }
    }
}
