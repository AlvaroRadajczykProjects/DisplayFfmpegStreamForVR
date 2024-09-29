package com.example.displayffmpegstreamforvr

import android.content.Intent
import android.os.Bundle
import android.view.TextureView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.example.displayffmpegstreamforvr.renderer.ReplicatingRendererFactory
import com.example.displayffmpegstreamforvr.util.WifiUtil
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLivePlaybackSpeedControl
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy
import com.google.android.exoplayer2.upstream.UdpDataSource
import kotlin.math.ceil

class MainActivity : ComponentActivity() {
    private lateinit var exoPlayer: ExoPlayer

    private lateinit var drawView: TextureView
    private lateinit var toCopyViews: List<TextureView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        drawView = TextureView(this)
        toCopyViews = listOf(TextureView(this))

        //Create two texture views renderer
        val rendererFactory: RenderersFactory = ReplicatingRendererFactory(
            this,
            drawView,
            toCopyViews
        )

        // Initialize the ExoPlayer
        exoPlayer = ExoPlayer.Builder(this).setLoadControl(createLowBufferLoadControl())
            .setRenderersFactory(rendererFactory)
            .setLivePlaybackSpeedControl(createLivePlaybackSpeedControl())
            .setTrackSelector(createTrackSelector())
            .build()

        // Set faster playback speed to reduce buffering as much as possible
        exoPlayer.playbackParameters = PlaybackParameters(1.02f)

        WifiUtil.getIpv4Address(this)?.let { ipAddress ->
            // Load the UDP Stream and prepare the player
            val streamUrl = "udp://$ipAddress:1234"
            val mediaSource = buildUdpMediaSource(streamUrl)
            Toast.makeText(this, "Ip address: $ipAddress", Toast.LENGTH_LONG).show()
            // Start the ExoPlayer with UDP MediaSource
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.setVideoTextureView(drawView)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }

        // Set up the content with Jetpack Compose
        setContent {
            UDPStreamPlayerView(
                drawView,
                toCopyViews,
                //Change this to your video stream dimensions
                //Both views must fit in screen. Otherwise, only one will be displayed
                1020,
                574,
                12
            ) {
                startActivity(Intent(applicationContext, CameraActivity::class.java))
            }
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
        val minBufferMs = 100   // Minimum buffer before playback starts
        val maxBufferMs = 200  // Maximum buffer during playback
        val bufferForPlaybackMs = 50  // Buffer to start playback
        val bufferForPlaybackAfterRebufferMs = 100  // Buffer after rebuffering

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
fun UDPStreamPlayerView(
    drawnView: TextureView,
    toCopyViews: List<TextureView>,
    width: Int,
    height: Int,
    distance: Int,
    onDoubleTap: () -> Unit = {}
) {
    val sep = ceil(((distance * 1f) / 2f).toDouble()).toFloat()
    val density = LocalDensity.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        onDoubleTap.invoke()
                    }
                )
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AndroidView(
            modifier = Modifier
                .width(with(density) { width.toDp() })
                .height(with(density) { height.toDp() })
                .padding(end = with(density) { sep.toDp() })
                .background(Color.Black),
            factory = { _ -> drawnView },
            update = {}
        )
        toCopyViews.forEach { view ->
            AndroidView(
                modifier = Modifier
                    .width(with(density) { width.toDp() })
                    .height(with(density) { height.toDp() })
                    .padding(start = with(density) { sep.toDp() })
                    .background(Color.Black),
                factory = { _ -> view },
                update = {}
            )
        }
    }
}