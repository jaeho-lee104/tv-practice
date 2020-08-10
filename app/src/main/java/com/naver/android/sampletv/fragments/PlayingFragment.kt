package com.naver.android.sampletv.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.leanback.app.PlaybackSupportFragment
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.PlaybackGlue
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.PlaybackControlsRow
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import coil.Coil
import coil.api.get
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.naver.android.sampletv.R
import com.naver.android.sampletv.models.TvMediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min


/**
 * @author leejaeho on 2020. 08. 07..
 */
class PlayingFragment : VideoSupportFragment() {

    /** AndroidX navigation arguments */
    private val args: PlayingFragmentArgs by navArgs()

    private lateinit var player: SimpleExoPlayer

    /** Glue layer between the player and our UI */
    private lateinit var playerGlue: MediaPlayerGlue


    /** Custom implementation of [PlaybackTransportControlGlue] */
    private inner class MediaPlayerGlue(context: Context, adapter: LeanbackPlayerAdapter) :
        PlaybackTransportControlGlue<LeanbackPlayerAdapter>(context, adapter) {

        private val actionRewind = PlaybackControlsRow.RewindAction(context)
        private val actionFastForward = PlaybackControlsRow.FastForwardAction(context)
        private val actionClosedCaptions = PlaybackControlsRow.ClosedCaptioningAction(context)

        fun skipForward(millis: Long = SKIP_PLAYBACK_MILLIS) =
            // Ensures we don't advance past the content duration (if set)
            player.seekTo(
                if (player.contentDuration > 0) {
                    min(player.contentDuration, player.currentPosition + millis)
                } else {
                    player.currentPosition + millis
                }
            )

        fun skipBackward(millis: Long = SKIP_PLAYBACK_MILLIS) =
            // Ensures we don't go below zero position
            player.seekTo(max(0, player.currentPosition - millis))

        override fun onCreatePrimaryActions(adapter: ArrayObjectAdapter) {
            super.onCreatePrimaryActions(adapter)
            // Append rewind and fast forward actions to our player, keeping the play/pause actions
            // created by default by the glue
            adapter.add(actionRewind)
            adapter.add(actionFastForward)
            adapter.add(actionClosedCaptions)
        }

        override fun onActionClicked(action: Action) = when (action) {
            actionRewind -> skipBackward()
            actionFastForward -> skipForward()
            else -> super.onActionClicked(action)
        }

        /** Custom function used to update the metadata displayed for currently playing media */
        fun setMetadata(metadata: TvMediaMetadata) {
            // Displays basic metadata in the player
            title = metadata.title
            subtitle = metadata.author
            lifecycleScope.launch(Dispatchers.IO) {
                metadata.artUri?.let { art = Coil.get(it) }
            }

            // Prepares metadata playback
            val dataSourceFactory = DefaultDataSourceFactory(
                requireContext(), getString(R.string.app_name)
            )
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(metadata.contentUri)
            player.prepare(mediaSource, false, true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backgroundType = PlaybackSupportFragment.BG_NONE
        val metadata = args.metadata

        // Initializes the video player
        player = ExoPlayerFactory.newSimpleInstance(requireContext())

        // Links our video player with this Leanback video playback fragment
        val playerAdapter = LeanbackPlayerAdapter(
            requireContext(), player, PLAYER_UPDATE_INTERVAL_MILLIS
        )

        // Enables pass-through of transport controls to our player instance
        playerGlue = MediaPlayerGlue(requireContext(), playerAdapter).apply {
            host = VideoSupportFragmentGlueHost(this@PlayingFragment)

            // Adds playback state listeners
            addPlayerCallback(object : PlaybackGlue.PlayerCallback() {

                override fun onPreparedStateChanged(glue: PlaybackGlue?) {
                    super.onPreparedStateChanged(glue)
                    if (glue?.isPrepared == true) {
                        // When playback is ready, skip to last known position
                        val startingPosition = metadata.playbackPositionMillis ?: 0
                        Log.d(TAG, "Setting starting playback position to $startingPosition")
                        seekTo(startingPosition)
                    }
                }
            })

            // Begins playback automatically
            playWhenPrepared()

            // Displays the current item's metadata
            setMetadata(metadata)
        }

        // Setup the fragment adapter with our player glue presenter
        adapter = ArrayObjectAdapter(playerGlue.playbackRowPresenter).apply {
            add(playerGlue.controlsRow)
        }

        // Adds key listeners
        playerGlue.host.setOnKeyInterceptListener { view, keyCode, event ->

            // Early exit: if the controls overlay is visible, don't intercept any keys
            if (playerGlue.host.isControlsOverlayVisible) return@setOnKeyInterceptListener false

            // TODO(owahltinez): This workaround is necessary for navigation library to work with
            //  Leanback's [PlaybackSupportFragment]
            if (!playerGlue.host.isControlsOverlayVisible &&
                keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN
            ) {
                Log.d(TAG, "Intercepting BACK key for fragment navigation")
                val navController = Navigation.findNavController(
                    requireActivity(), R.id.fragment_container
                )
                navController.currentDestination?.id?.let { navController.popBackStack(it, true) }
                return@setOnKeyInterceptListener true
            }

            // Skips ahead when user presses DPAD_RIGHT
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.action == KeyEvent.ACTION_DOWN) {
                playerGlue.skipForward()
                preventControlsOverlay(playerGlue)
                return@setOnKeyInterceptListener true
            }

            // Rewinds when user presses DPAD_LEFT
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && event.action == KeyEvent.ACTION_DOWN) {
                playerGlue.skipBackward()
                preventControlsOverlay(playerGlue)
                return@setOnKeyInterceptListener true
            }

            false
        }
    }

    /** Workaround used to prevent controls overlay from showing and taking focus */
    private fun preventControlsOverlay(playerGlue: MediaPlayerGlue) = view?.postDelayed({
        playerGlue.host.showControlsOverlay(false)
        playerGlue.host.hideControlsOverlay(false)
    }, 10)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(Color.BLACK)
    }

    /**
     * Deactivates and removes callbacks from [MediaSessionCompat] since the [Player] instance is
     * destroyed in onStop and required metadata could be missing.
     */
    override fun onPause() {
        super.onPause()

        playerGlue.pause()

    }

    companion object {
        private val TAG = PlayingFragment::class.java.simpleName

        /** How often the player refreshes its views in milliseconds */
        private const val PLAYER_UPDATE_INTERVAL_MILLIS: Int = 100

        /** Time between metadata updates in milliseconds */
        private val METADATA_UPDATE_INTERVAL_MILLIS: Long = TimeUnit.SECONDS.toMillis(10)

        /** Default time used when skipping playback in milliseconds */
        private val SKIP_PLAYBACK_MILLIS: Long = TimeUnit.SECONDS.toMillis(10)
    }
}