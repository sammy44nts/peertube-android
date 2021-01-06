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
package net.schueller.peertube

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import net.schueller.peertube.R
import java.util.*

abstract class CommonActivity : AppCompatActivity {
    constructor(@LayoutRes contentLayoutId: Int): super(contentLayoutId)

    constructor(): super()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val nightMode = if (sharedPref.getBoolean(getString(R.string.pref_dark_mode_key), false)) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(nightMode)
        setTheme(resources.getIdentifier(
                sharedPref.getString(
                        getString(R.string.pref_theme_key),
                        getString(R.string.app_default_theme)
                ),
                "style",
                packageName)
        )
        // Neither Chinese language choice was working, found this fix on stack overflow
        when (val countryCode = sharedPref.getString(getString(R.string.pref_language_app_key), "en")!!) {
            "zh-rCN" -> Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
            "zh-rTW" -> Locale.setDefault(Locale.TRADITIONAL_CHINESE)
            else -> Locale.setDefault(Locale(countryCode))
        }
        val config = baseContext.resources.configuration
        config.locale = Locale.getDefault()
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
    }
}
