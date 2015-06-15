package com.carltondennis.spotifystreamer.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.carltondennis.spotifystreamer.R;
import com.carltondennis.spotifystreamer.data.SpotifyArtist;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by alex on 5/30/15.
 */
public class ArtistsAdapter extends ArrayAdapter<SpotifyArtist> {

    private Context mContext;
    private int mResource;
    private ArrayList<SpotifyArtist> mItems;

    public ArtistsAdapter(Context context, int resource, ArrayList<SpotifyArtist> items) {

        super(context, resource, items);

        mContext = context;
        mResource = resource;
        mItems = items;
    }

    public ArrayList<SpotifyArtist> getArtists() {
        return mItems;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder viewHolder;

        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(mResource, parent, false);
            viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        SpotifyArtist artist = getItem(position);
        viewHolder.nameView.setText(artist.name);
        if (artist.imageURL != null) {
            Picasso.with(mContext).load(artist.imageURL).into(viewHolder.imageView);
        } else {
            Picasso.with(mContext).load(R.drawable.ic_social_person).into(viewHolder.imageView);
        }

        return view;
    }

    /**
     * Cache of the children views for a forecast list item.
     */
    public static class ViewHolder {
        public final ImageView imageView;
        public final TextView nameView;

        public ViewHolder(View view) {
            imageView = (ImageView) view.findViewById(R.id.list_item_image_artist);
            nameView = (TextView) view.findViewById(R.id.list_item_name_artist);
        }
    }
}
