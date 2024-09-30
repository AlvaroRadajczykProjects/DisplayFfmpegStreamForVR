package com.example.displayffmpegstreamforvr.renderer

import android.content.Context
import android.graphics.Paint
import android.view.TextureView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer

@UnstableApi
class ReplicatingRenderer(
    context: Context,
    mediaCodecSelector: MediaCodecSelector,
    private val drawnTextureView: TextureView,
    private val toDrawTextureViews: List<TextureView>
) : MediaCodecVideoRenderer(context, mediaCodecSelector) {
    private val paint = Paint()

    override fun onProcessedOutputBuffer(presentationTimeUs: Long) {
        super.onProcessedOutputBuffer(presentationTimeUs)
        copyFrame()
    }

    private fun copyFrame() {
        val bitmap = drawnTextureView.bitmap ?: return
        toDrawTextureViews.forEach { view ->
            if (view.width != drawnTextureView.width || view.height != drawnTextureView.height) return@forEach
            val canvas = view.lockCanvas() ?: return@forEach
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            view.unlockCanvasAndPost(canvas)
        }
        bitmap.recycle()
    }
}