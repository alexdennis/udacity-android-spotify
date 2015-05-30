package com.carltondennis.spotifystreamer;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;


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
            }
        }
    }
}
