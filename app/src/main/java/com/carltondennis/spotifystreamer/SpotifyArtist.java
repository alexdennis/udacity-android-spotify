package com.carltondennis.spotifystreamer;

/**
 * Created by alex on 6/3/15.
 */
public class SpotifyArtist {
    public String name;
    public String imageURL;
    public String spotifyId;

    public SpotifyArtist(String name, String imageURL, String spotifyId) {
        this.name = name;
        this.imageURL = imageURL;
        this.spotifyId = spotifyId;
    }
}
