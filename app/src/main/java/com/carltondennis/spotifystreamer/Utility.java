package com.carltondennis.spotifystreamer;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by alex on 6/3/15.
 */
public class Utility {

    public static final int SECOND_IN_MILLISECONDS = 1000;

    public static String getPreferredCountry(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_country_key),
                context.getString(R.string.pref_country_default));
    }

    public static boolean isNotificationEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.pref_notification_key),
                Boolean.parseBoolean(context.getString(R.string.pref_notification_default)));
    }

    public static String fromMillisecs(int milliSeconds) {
        int minutes = (milliSeconds / SECOND_IN_MILLISECONDS) / 60;
        int seconds = (milliSeconds / SECOND_IN_MILLISECONDS) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
