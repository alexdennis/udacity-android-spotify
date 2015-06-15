package com.carltondennis.spotifystreamer.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.carltondennis.spotifystreamer.R;
import com.carltondennis.spotifystreamer.data.SpotifyTrack;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by alex on 6/2/15.
 */
public class TracksAdapter extends ArrayAdapter<SpotifyTrack> {

    private Context mContext;
    private int mResource;
    private ArrayList<SpotifyTrack> mItems;

    public TracksAdapter(Context context, int resource, ArrayList<SpotifyTrack> items) {

        super(context, resource, items);

        mContext = context;
        mResource = resource;
        mItems = items;
    }

    public ArrayList<SpotifyTrack> getTracks() {
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

        SpotifyTrack track = getItem(position);
        viewHolder.nameView.setText(track.name);
        viewHolder.albumNameView.setText(track.albumName);
        if (track.imageSmallURL != null) {
            Picasso.with(mContext).load(track.imageSmallURL).into(viewHolder.imageView);
        } else {
            Picasso.with(mContext).load(R.drawable.ic_av_album).into(viewHolder.imageView);
        }

        return view;
    }

    /**
     * Cache of the children views for a forecast list item.
     */
    public static class ViewHolder {
        public final ImageView imageView;
        public final TextView nameView;
        public final TextView albumNameView;

        public ViewHolder(View view) {
            imageView = (ImageView) view.findViewById(R.id.list_item_image_album);
            nameView = (TextView) view.findViewById(R.id.list_item_name_track);
            albumNameView = (TextView) view.findViewById(R.id.list_item_name_album);
        }
    }
}
