/*
 * Copyright (C) 2020 Stefan Schüller <sschueller@techdroid.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.schueller.peertube.network;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import net.schueller.peertube.R;

import static net.schueller.peertube.service.LoginService.refreshToken;

public class Session {
    private static volatile Session sSoleInstance;
    private final SharedPreferences sharedPreferences;
    private final String prefAuthUsername;
    private final String prefAuthPassword;
    private final String prefTokenAccess;
    private final String prefTokenRefresh;
    private final String prefTokenType;

    // private constructor.
    private Session(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        prefAuthUsername = context.getString(R.string.pref_auth_username);
        prefAuthPassword = context.getString(R.string.pref_auth_password);
        prefTokenAccess = context.getString(R.string.pref_token_access);
        prefTokenRefresh = context.getString(R.string.pref_token_refresh);
        prefTokenType = context.getString(R.string.pref_token_type);

        // Prevent from the reflection api.
        if (sSoleInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static Session getInstance(Context context) {
        if (sSoleInstance == null) { // if there is no instance available... create new one
            synchronized (Session.class) {
                if (sSoleInstance == null) sSoleInstance = new Session(context);
            }
        }

        return sSoleInstance;
    }

    public boolean isLoggedIn() {
        // check if token exist or not
        // return true if exist otherwise false
        // assuming that token exists

        // Log.v("Session", "isLoggedIn: " + (getToken() != null));

        return getToken() != null;
    }

    public String getToken() {
        // return the token that was saved earlier
        String token = sharedPreferences.getString(prefTokenAccess, null);
        String type = sharedPreferences.getString(prefTokenType, "Bearer");
        if (token != null) return type + " " + token;
        else return null;
    }

    public String getPassword() {
        return sharedPreferences.getString(prefAuthPassword, null);

    }

    public String getRefreshToken() {
        return sharedPreferences.getString(prefTokenRefresh, null);

    }

    public String refreshAccessToken(Context context) {
        refreshToken(context);
        return getToken();
    }

    public void invalidate() {
        // get called when user become logged out
        // delete token and other user info
        // (i.e: email, password)
        // from the storage
        sharedPreferences.edit()
                .putString(prefAuthUsername, null)
                .putString(prefAuthPassword, null)
                .putString(prefTokenAccess, null)
                .putString(prefTokenRefresh, null)
                .apply();
    }
}