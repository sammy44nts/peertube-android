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
package net.schueller.peertube.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import net.schueller.peertube.R;
import net.schueller.peertube.helper.APIUrlHelper;
import net.schueller.peertube.model.OauthClient;
import net.schueller.peertube.model.Token;
import net.schueller.peertube.network.AuthenticationService;
import net.schueller.peertube.network.RetrofitInstance;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginService {
    private static final String TAG = "LoginService";

    public static void Authenticate(Context context, String username, String password) {
        String apiBaseURL = APIUrlHelper.getUrlWithVersion(context);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        AuthenticationService service = RetrofitInstance
                .getRetrofitInstance(context, apiBaseURL, APIUrlHelper.useInsecureConnection(context))
                .create(AuthenticationService.class);

        Call<OauthClient> call = service.getOauthClientLocal();

        call.enqueue(new Callback<OauthClient>() {
            @Override
            public void onResponse(@NonNull Call<OauthClient> call, @NonNull Response<OauthClient> response) {
                if (response.isSuccessful()) {
                    OauthClient oauthClient = response.body();
                    assert oauthClient != null;
                    sharedPref.edit()
                            .putString(context.getString(R.string.pref_client_id), oauthClient.getClientId())
                            .putString(context.getString(R.string.pref_client_secret), oauthClient.getClientSecret())
                            .apply();
                    Call<Token> call2 = service.getAuthenticationToken(
                            oauthClient.getClientId(),
                            oauthClient.getClientSecret(),
                            "code",
                            "password",
                            "upload",
                            username,
                            password
                    );
                    call2.enqueue(new Callback<Token>() {
                        @Override
                        public void onResponse(@NonNull Call<Token> call2, @NonNull retrofit2.Response<Token> response2) {
                            if (response2.isSuccessful()) {
                                Token token = response2.body();
                                assert token != null;
                                sharedPref.edit()
                                        .putString(context.getString(R.string.pref_token_access), token.getAccessToken())
                                        .putString(context.getString(R.string.pref_token_refresh), token.getRefreshToken())
                                        .putString(context.getString(R.string.pref_token_type), token.getTokenType())
                                        .apply();
                                Log.wtf(TAG, "Logged in");
                                Toast.makeText(context, context.getString(R.string.authentication_login_success), Toast.LENGTH_LONG).show();
                            } else {
                                Log.wtf(TAG, response2.toString());
                                Toast.makeText(context, context.getString(R.string.authentication_login_failed), Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Token> call2, @NonNull Throwable t2) {
                            Log.wtf("err", t2.fillInStackTrace());
                            Toast.makeText(context, context.getString(R.string.authentication_login_failed), Toast.LENGTH_LONG).show();

                        }
                    });

                } else {
                    // Log.wtf(TAG, response.toString());
                    Toast.makeText(context, context.getString(R.string.authentication_login_failed), Toast.LENGTH_LONG).show();

                }
            }

            @Override
            public void onFailure(@NonNull Call<OauthClient> call, @NonNull Throwable t) {
                // Log.wtf("err", t.fillInStackTrace());
                Toast.makeText(context, context.getString(R.string.authentication_login_failed), Toast.LENGTH_LONG).show();

            }
        });
    }

    public static void refreshToken(Context context) {
        String apiBaseURL = APIUrlHelper.getUrlWithVersion(context);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        AuthenticationService service = RetrofitInstance
                .getRetrofitInstance(context, apiBaseURL, APIUrlHelper.useInsecureConnection(context))
                .create(AuthenticationService.class);

        String refreshToken = sharedPref.getString(context.getString(R.string.pref_token_refresh), null);
        String userName = sharedPref.getString(context.getString(R.string.pref_auth_username), null);
        String clientId = sharedPref.getString(context.getString(R.string.pref_client_id), null);
        String clientSecret = sharedPref.getString(context.getString(R.string.pref_client_secret), null);

        Call<Token> call = service.refreshToken(
                clientId,
                clientSecret,
                "refresh_token",
                "code",
                userName,
                refreshToken
        );
        call.enqueue(new Callback<Token>() {
            @Override
            public void onResponse(@NonNull Call<Token> call, @NonNull retrofit2.Response<Token> response) {
                if (response.isSuccessful()) {
                    Token token = response.body();
                    assert token != null;
                    sharedPref.edit()
                            .putString(context.getString(R.string.pref_token_access), token.getAccessToken())
                            .putString(context.getString(R.string.pref_token_refresh), token.getRefreshToken())
                            .putString(context.getString(R.string.pref_token_type), token.getTokenType())
                            .apply();
                    Log.wtf(TAG, "Logged in");
                    Toast.makeText(context, context.getString(R.string.authentication_token_refresh_success), Toast.LENGTH_LONG).show();
                } else {
                    // Log.wtf(TAG, response.toString());
                    Toast.makeText(context, context.getString(R.string.authentication_token_refresh_failed), Toast.LENGTH_LONG).show();

                }
            }

            @Override
            public void onFailure(@NonNull Call<Token> call2, @NonNull Throwable t2) {
                // Log.wtf("err", t2.fillInStackTrace());
                Toast.makeText(context, context.getString(R.string.authentication_token_refresh_failed), Toast.LENGTH_LONG).show();

            }
        });
    }
}
