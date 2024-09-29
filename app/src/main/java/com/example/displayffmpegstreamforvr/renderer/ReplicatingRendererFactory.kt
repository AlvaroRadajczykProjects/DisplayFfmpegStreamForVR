package com.example.displayffmpegstreamforvr.renderer

import android.content.Context
import android.os.Handler
import android.view.TextureView
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.video.VideoRendererEventListener

class ReplicatingRendererFactory(
    context: Context,
    private val drawnTextureView: TextureView,
    private val toDrawTextureViews: List<TextureView>
) : DefaultRenderersFactory(context) {
    override fun buildVideoRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        eventHandler: Handler,
        eventListener: VideoRendererEventListener,
        allowedVideoJoiningTimeMs: Long,
        out: ArrayList<Renderer>
    ) {
        out.add(
            ReplicatingRenderer(
                context,
                mediaCodecSelector,
                drawnTextureView,
                toDrawTextureViews
            )
        )
        super.buildVideoRenderers(
            context,
            extensionRendererMode,
            mediaCodecSelector,
            enableDecoderFallback,
            eventHandler,
            eventListener,
            allowedVideoJoiningTimeMs,
            out
        )
    }
}