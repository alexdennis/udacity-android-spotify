package com.carltondennis.spotifystreamer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by alex on 6/3/15.
 */
public class SpotifyTrack implements Parcelable {
    public String name;
    public String albumName;
    public String imageLargeURL;
    public String imageSmallURL;
    public String previewURL;

    public SpotifyTrack(String name, String albumName, String imageLargeURL, String imageSmallURL, String previewURL) {
        this.name = name;
        this.albumName = albumName;
        this.imageLargeURL = imageLargeURL;
        this.imageSmallURL = imageSmallURL;
        this.previewURL = previewURL;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(name);
        out.writeString(albumName);
        out.writeString(imageLargeURL);
        out.writeString(imageSmallURL);
        out.writeString(previewURL);
    }

    public static final Parcelable.Creator<SpotifyTrack> CREATOR
            = new Parcelable.Creator<SpotifyTrack>() {
        public SpotifyTrack createFromParcel(Parcel in) {
            return new SpotifyTrack(in);
        }

        public SpotifyTrack[] newArray(int size) {
            return new SpotifyTrack[size];
        }
    };

    private SpotifyTrack(Parcel in) {
        name = in.readString();
        albumName = in.readString();
        imageLargeURL = in.readString();
        imageSmallURL = in.readString();
        previewURL = in.readString();
    }

}