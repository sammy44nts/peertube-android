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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.squareup.picasso.Picasso
import net.schueller.peertube.activity.SettingsActivity
import net.schueller.peertube.helper.APIUrlHelper
import net.schueller.peertube.helper.ErrorHelper
import net.schueller.peertube.model.Me
import net.schueller.peertube.network.GetUserService
import net.schueller.peertube.network.RetrofitInstance
import net.schueller.peertube.network.Session
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class MeActivity : CommonActivity(R.layout.activity_me) {
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_top_account, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        finish() // close this activity as oppose to navigating up
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val toolbar: Toolbar = findViewById(R.id.tool_bar_me)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_baseline_close_24)
        }
        val account: LinearLayout = findViewById(R.id.a_me_account_line)
        val settings: LinearLayout = findViewById(R.id.a_me_settings)
        val help: LinearLayout = findViewById(R.id.a_me_helpnfeedback)
        val logout: TextView = findViewById(R.id.a_me_logout)
        settings.setOnClickListener {
            Intent(this, SettingsActivity::class.java).apply {
                // overridePendingTransition(R.anim.slide_in_bottom, 0);
                startActivity(this)
            }
        }
        help.setOnClickListener {
            val url = "https://github.com/sschueller/peertube-android/issues"
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                startActivity(this)
            }
        }
        logout.setOnClickListener {
            Session.getInstance(this).invalidate()
            account.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        getUserData()
    }

    // TODO: move network call to a repository -> MVVM style
    private fun getUserData() {
        val apiBaseURL = APIUrlHelper.getUrlWithVersion(this)
        val baseURL = APIUrlHelper.getUrl(this)
        val service = RetrofitInstance
                .getRetrofitInstance(this, apiBaseURL, APIUrlHelper.useInsecureConnection(this))
                .create(GetUserService::class.java)
        val call = service.me
        call.enqueue(object : Callback<Me> {
            val account = findViewById<LinearLayout>(R.id.a_me_account_line)
            override fun onResponse(call: Call<Me>, response: Response<Me>) {
                if (response.isSuccessful) {
                    response.body()?.also { me ->
                        Log.d("${this@MeActivity}", "${response.body()}")
                        val username: TextView = findViewById(R.id.a_me_username)
                        val email: TextView = findViewById(R.id.a_me_email)
                        val avatarView: ImageView = findViewById(R.id.a_me_avatar)
                        username.text = me.username
                        email.text = me.email
                        val avatar = me.account.avatar
                        if (avatar != null) {
                            val avatarPath = avatar.path
                            Picasso.get().load(baseURL + avatarPath).into(avatarView)
                        }
                    }
                    account.visibility = View.VISIBLE
                } else account.visibility = View.GONE
            }

            override fun onFailure(call: Call<Me>, t: Throwable) {
                ErrorHelper.showToastFromCommunicationError(this@MeActivity, t)
                account.visibility = View.GONE
            }
        })
    }
}
