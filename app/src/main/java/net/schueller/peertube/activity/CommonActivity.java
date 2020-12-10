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

package net.schueller.peertube.activity;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import net.schueller.peertube.R;

import java.util.Locale;

public class CommonActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set Night Mode
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        AppCompatDelegate.setDefaultNightMode(sharedPref.getBoolean(getString(R.string.pref_dark_mode_key), false) ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        // Set theme
        setTheme(getResources().getIdentifier(
                sharedPref.getString(
                        getString(R.string.pref_theme_key),
                        getString(R.string.app_default_theme)
                ),
                "style",
                getPackageName())
        );

        // Set language
        String countryCode = sharedPref.getString(getString(R.string.pref_language_app_key), "en");
        assert countryCode != null;
        Locale locale = new Locale(countryCode);

        //Neither Chinese language choice was working, found this fix on stack overflow
        if (countryCode.equals("zh-rCN"))
            locale = Locale.SIMPLIFIED_CHINESE;
        if (countryCode.equals("zh-rTW"))
            locale = Locale.TRADITIONAL_CHINESE;

        Locale.setDefault(locale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
    }

}
