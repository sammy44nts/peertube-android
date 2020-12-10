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

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static net.schueller.peertube.network.UnsafeOkHttpClient.getUnsafeOkHttpClientBuilder;

public class RetrofitInstance {

    private static Retrofit retrofit;
    private static String baseUrl;

    public static Retrofit getRetrofitInstance(String newBaseUrl, boolean insecure) {
        if (retrofit == null || !newBaseUrl.equals(baseUrl)) {
            baseUrl = newBaseUrl;

            OkHttpClient.Builder okhttpClientBuilder;

            if (!insecure) {
                okhttpClientBuilder = new OkHttpClient.Builder();
            } else {
                okhttpClientBuilder = getUnsafeOkHttpClientBuilder();
            }

            okhttpClientBuilder.addInterceptor(new AuthorizationInterceptor());
            okhttpClientBuilder.authenticator(new AccessTokenAuthenticator());

            retrofit = new retrofit2.Retrofit.Builder()
                    .client(okhttpClientBuilder.build())
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}