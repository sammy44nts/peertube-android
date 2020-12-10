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
package net.schueller.peertube.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.webkit.URLUtil
import android.widget.Toast
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.C.CONTENT_TYPE_MOVIE
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.PlayerNotificationManager.BitmapCallback
import com.google.android.exoplayer2.ui.PlayerNotificationManager.MediaDescriptionAdapter
import com.google.android.exoplayer2.util.Util
import net.schueller.peertube.R
import net.schueller.peertube.activity.VideoListActivity
import net.schueller.peertube.activity.VideoPlayActivity
import net.schueller.peertube.helper.APIUrlHelper
import net.schueller.peertube.helper.MetaDataHelper
import net.schueller.peertube.model.Video
import net.schueller.peertube.network.UnsafeOkHttpClient
import okhttp3.OkHttpClient

class VideoPlayerService : Service() {
    val player: SimpleExoPlayer
        get() = _player
    var playBackSpeed: Float
        get() = player.playbackParameters.speed
        set(speed) {
            Log.v(TAG, "setPlayBackSpeed...")
            player.setPlaybackParameters(PlaybackParameters(speed))
        }
    private val audioAttributes = AudioAttributes.Builder().run {
        setContentType(CONTENT_TYPE_MOVIE)
        build()
    }
    private val mediaDescriptionAdapter = object : MediaDescriptionAdapter {
        override fun getCurrentContentTitle(player: Player) = currentVideo.name

        override fun createCurrentContentIntent(player: Player): PendingIntent? {
            return Intent(applicationContext, VideoPlayActivity::class.java).let {
                it.putExtra(VideoListActivity.EXTRA_VIDEOID, currentVideo.uuid)
                PendingIntent.getActivity(
                        applicationContext,
                        0,
                        it,
                        PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
        }

        override fun getCurrentContentText(player: Player): String? {
            return MetaDataHelper.getMetaString(
                    currentVideo.createdAt,
                    currentVideo.views,
                    applicationContext
            )
        }

        override fun getCurrentLargeIcon(player: Player, callback: BitmapCallback): Bitmap? {
            return null
        }
    }
    private val notificationListener = object : PlayerNotificationManager.NotificationListener {
        override fun onNotificationStarted(notificationId: Int, notification: Notification) {
            startForeground(notificationId, notification)
        }

        override fun onNotificationCancelled(notificationId: Int) {
            Log.v(TAG, "onNotificationCancelled...")
            stopForeground(true)
            val killFloat = Intent(PlayerNotificationManager.ACTION_STOP)
            sendBroadcast(killFloat)
            /*
            Intent killFloat = new Intent(BROADCAST_ACTION);
            Intent killFloatingWindow = new Intent(getApplicationContext(),VideoPlayActivity.class);
            killFloatingWindow.putExtra("killFloat",true);

            startActivity(killFloatingWindow);
            // TODO: only kill the notification if we no longer have a bound activity
            stopForeground(true);
        */
        }
    }
    private val mBinder: IBinder = LocalBinder()
    private val becomeNoisyIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private val myNoisyAudioStreamReceiver = BecomingNoisyReceiver()
    private lateinit var _player: SimpleExoPlayer
    private lateinit var currentVideo: Video
    private lateinit var currentStreamUrl: String
    private lateinit var playerNotificationManager: PlayerNotificationManager

    override fun onCreate() {
        super.onCreate()
        Log.v(TAG, "onCreate...")
        _player = SimpleExoPlayer.Builder(applicationContext).build()
        // Stop player if audio device changes, e.g. headphones unplugged
        player.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState.toLong() == PlaybackState.ACTION_PAUSE) { // this means that pause is available, hence the audio is playing
                    Log.v(TAG, "ACTION_PLAY: $playbackState")
                    registerReceiver(myNoisyAudioStreamReceiver, becomeNoisyIntentFilter)
                }
                if (playbackState
                                .toLong() == PlaybackState.ACTION_PLAY) { // this means that play is available, hence the audio is paused or stopped
                    Log.v(TAG, "ACTION_PAUSE: $playbackState")
                    safeUnregisterReceiver()
                }
            }
        })
    }

    override fun onBind(intent: Intent) = mBinder

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.v(TAG, "onStartCommand...")
        return if (!URLUtil.isValidUrl(currentStreamUrl)) {
            Toast.makeText(
                    this,
                    "Invalid URL provided. Unable to play video.",
                    Toast.LENGTH_SHORT
            ).show()
            START_NOT_STICKY
        } else {
            playVideo()
            START_STICKY
        }
    }

    override fun onDestroy() {
        Log.v(TAG, "onDestroy...")
        if (this::playerNotificationManager.isInitialized) playerNotificationManager.setPlayer(null)
        // Was seeing an error when exiting the program about not unregistering the receiver.
        safeUnregisterReceiver()
        player.release()
        super.onDestroy()
    }

    fun setCurrentVideo(video: Video) {
        Log.v(TAG, "setCurrentVideo...")
        currentVideo = video
    }

    fun setCurrentStreamUrl(streamUrl: String) {
        Log.v(TAG, "setCurrentStreamUrl...$streamUrl")
        currentStreamUrl = streamUrl
    }

    fun playVideo() {
        // We need a valid URL
        Log.v(TAG, "playVideo...")

        // Produces DataSource instances through which media data is loaded.
//        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(getApplicationContext(),
//                Util.getUserAgent(getApplicationContext(), "PeerTube"), null);
        val okhttpClient = if (!APIUrlHelper.useInsecureConnection(this)) {
            OkHttpClient.Builder().build()
        } else {
            UnsafeOkHttpClient.getUnsafeOkHttpClientBuilder().build()
        }
        // This is the MediaSource representing the media to be played.
        ProgressiveMediaSource.Factory(OkHttpDataSourceFactory(
                okhttpClient,
                Util.getUserAgent(applicationContext, "PeerTube")
        )).createMediaSource(MediaItem.fromUri(Uri.parse(currentStreamUrl))).also {
            player.setAudioAttributes(audioAttributes, true)
            // Prepare the player with the source.
            player.setMediaSource(it)
            player.prepare()
        }
        // Auto play
        player.playWhenReady = true
        //reset playback speed
        playBackSpeed = 1.0f
        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
                this,
                PLAYBACK_CHANNEL_ID,
                R.string.playback_channel_name,
                R.string.playback_channel_name,
                PLAYBACK_NOTIFICATION_ID,
                mediaDescriptionAdapter,
                notificationListener
        ).apply {
            setSmallIcon(R.drawable.ic_logo_bw)
            // don't show skip buttons in notification
            setUseNavigationActions(false)
            setUseStopAction(true)
            setPlayer(player)
        }
        // external Media control, Android Wear / Google Home etc.
        val mediaSession = MediaSessionCompat(this, MEDIA_SESSION_TAG)
        mediaSession.isActive = true
        playerNotificationManager.setMediaSessionToken(mediaSession.sessionToken)
        val mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setQueueNavigator(object : TimelineQueueNavigator(mediaSession) {
            override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
                return Video.getMediaDescription(applicationContext, currentVideo)
            }
        })
        mediaSessionConnector.setPlayer(player)
    }

    private fun safeUnregisterReceiver() {
        try {
            unregisterReceiver(myNoisyAudioStreamReceiver)
        } catch (e: Exception) {
            Log.e("VideoPlayerService", "attempted to unregister a nonregistered service")
        }
    }

    inner class LocalBinder : Binder() {
        // Return this instance of VideoPlayerService so clients can call public methods
        val service = this@VideoPlayerService
    }

    // pause playback on audio output change
    private inner class BecomingNoisyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                player.playWhenReady = false
            }
        }
    }

    companion object {
        private const val TAG = "VideoPlayerService"
        private const val MEDIA_SESSION_TAG = "peertube_player"
        private const val PLAYBACK_CHANNEL_ID = "playback_channel"
        private const val PLAYBACK_NOTIFICATION_ID = 1
    }
}