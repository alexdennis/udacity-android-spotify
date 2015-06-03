package com.carltondennis.spotifystreamer;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

/**
 * Created by alex on 6/2/15.
 */
public class TracksAdapter extends CursorAdapter {

    public TracksAdapter(Context context, Cursor c, int flags) {

        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_tracks, parent, false);
        view.setTag(new ViewHolder(view));
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder viewHolder = (ViewHolder) view.getTag();

        String name  = cursor.getString(TracksActivityFragment.COL_TRACK_NAME);
        String albumName = cursor.getString(TracksActivityFragment.COL_TRACK_ALBUM);
        String image = cursor.getString(TracksActivityFragment.COL_TRACK_ALBUM_IMAGE_SMALL);

        viewHolder.nameView.setText(name);
        viewHolder.albumNameView.setText(albumName);
        if (image != null) {
            Picasso.with(context).load(image).into(viewHolder.imageView);
        }

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
