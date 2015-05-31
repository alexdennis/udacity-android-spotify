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
 * Created by alex on 5/30/15.
 */
public class ArtistsAdapter extends CursorAdapter {

    public ArtistsAdapter(Context context, Cursor c, int flags) {

        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_artist, parent, false);
        view.setTag(new ViewHolder(view));
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder viewHolder = (ViewHolder) view.getTag();

        String name = cursor.getString(2);
        String image = cursor.getString(1);

        viewHolder.nameView.setText(name);
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

        public ViewHolder(View view) {
            imageView = (ImageView) view.findViewById(R.id.list_item_image_artist);
            nameView = (TextView) view.findViewById(R.id.list_item_name_artist);
        }
    }
}
