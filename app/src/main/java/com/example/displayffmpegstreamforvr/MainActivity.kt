package com.example.displayffmpegstreamforvr

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.displayffmpegstreamforvr.util.WifiUtil
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLivePlaybackSpeedControl
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy
import com.google.android.exoplayer2.upstream.UdpDataSource

class MainActivity : ComponentActivity() {
    private lateinit var exoPlayer: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the ExoPlayer
        exoPlayer = ExoPlayer.Builder(this).setLoadControl(createLowBufferLoadControl())
            .setLivePlaybackSpeedControl(createLivePlaybackSpeedControl()).setTrackSelector(createTrackSelector()).build()

        // Set faster playback speed to reduce buffering as much as possible
        exoPlayer.playbackParameters = PlaybackParameters(1.02f)

        WifiUtil.getIpv4Address(this)?.let { ipAddress ->
            // Load the UDP Stream and prepare the player
            val streamUrl = "udp://$ipAddress:1234"
            val mediaSource = buildUdpMediaSource(streamUrl)
            Toast.makeText(this, "Ip address: $streamUrl", Toast.LENGTH_LONG).show()
            // Start the ExoPlayer with UDP MediaSource
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }

        // Set up the content with Jetpack Compose
        setContent {
            UDPStreamPlayer(exoPlayer)
        }
    }

    // Create a custom DataSource.Factory for UdpDataSource
    private fun buildUdpMediaSource(url: String): MediaSource {
        // Create a custom DataSource.Factory that uses UdpDataSource
        val udpDataSourceFactory = DataSource.Factory {
            UdpDataSource()
        }

        // Create ProgressiveMediaSource using the custom factory
        return ProgressiveMediaSource.Factory(udpDataSourceFactory).setLoadErrorHandlingPolicy(object : DefaultLoadErrorHandlingPolicy() {
            //Number of retries
            override fun getMinimumLoadableRetryCount(dataType: Int): Int {
                return 40
            }

            //Extra delay each retry (retry delay is near 1 sec)
            override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                return 500
            }
        }).createMediaSource(MediaItem.fromUri(url))
    }

    // Create a custom LoadControl to reduce buffering as much as possible
    private fun createLowBufferLoadControl(): LoadControl {
        // Create an allocator
        val allocator = DefaultAllocator(true, 16)

        // Set buffer durations in milliseconds
        // maxBufferMs is max an nothing can be higher
        val minBufferMs = 500   // Minimum buffer before playback starts
        val maxBufferMs = 1000  // Maximum buffer during playback
        val bufferForPlaybackMs = 250  // Buffer to start playback
        val bufferForPlaybackAfterRebufferMs = 500  // Buffer after rebuffering

        return DefaultLoadControl.Builder().setAllocator(allocator).setBufferDurationsMs(
            minBufferMs, maxBufferMs, bufferForPlaybackMs, bufferForPlaybackAfterRebufferMs
        ).setTargetBufferBytes(C.LENGTH_UNSET)  // Allow ExoPlayer to set buffer size dynamically
            .setPrioritizeTimeOverSizeThresholds(true)  // Prioritize buffering time over size
            .build()
    }

    private fun createLivePlaybackSpeedControl(): DefaultLivePlaybackSpeedControl {
        return DefaultLivePlaybackSpeedControl.Builder()
            //.setMaxLiveOffsetErrorMsForUnitSpeed()
            //.setTargetLiveOffsetIncrementOnRebufferMs()
            .setFallbackMinPlaybackSpeed(1.0f) //MAYOR 1.0F FALLA
            .setMinUpdateIntervalMs(66).build()
    }

    private fun createTrackSelector(): DefaultTrackSelector {
        val ts = DefaultTrackSelector(this)
        ts.setParameters(
            ts.buildUponParameters().setTunnelingEnabled(true)
                //.setTunnelingAudioSessionId(C.generateAudioSessionIdV21(this))
                .setMinVideoBitrate(524_288)  // Lower bitrate to ensure smooth live playback
                .setMaxVideoBitrate(2_097_152).setForceLowestBitrate(true).build()
        )

        return ts
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
    }
}

@Composable
fun UDPStreamPlayer(exoPlayer: ExoPlayer) {
    // Composable to display ExoPlayer's PlayerView
    AndroidView(modifier = Modifier.fillMaxSize(), factory = { context ->
        // Create a PlayerView and attach the ExoPlayer to it
        PlayerView(context).apply {
            player = exoPlayer
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
    }, update = {
        it.player = exoPlayer // Update the player if needed
    })
}