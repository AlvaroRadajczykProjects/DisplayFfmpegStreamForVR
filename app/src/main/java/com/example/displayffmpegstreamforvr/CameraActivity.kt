package com.example.displayffmpegstreamforvr

import android.content.Intent
import android.os.Bundle
import android.view.TextureView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import kotlin.math.ceil

class CameraActivity : ComponentActivity() {

    private lateinit var drawView: TextureView
    private lateinit var toCopyViews: List<TextureView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        drawView = TextureView(this)
        toCopyViews = listOf(TextureView(this))

        // Set up the content with Jetpack Compose
        setContent {
            CameraView(
                1020,
                574,
                12
            ) {
                startActivity(Intent(applicationContext, MainActivity::class.java))
            }
        }
    }
}

@Composable
fun CameraView(
    width: Int,
    height: Int,
    distance: Int,
    onDoubleTap: () -> Unit = {}
) {
    val sep = ceil(((distance * 1f) / 2f).toDouble()).toFloat()
    val density = LocalDensity.current
    val context = LocalContext.current

    var onLongPress = {}

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        onLongPress.invoke()
                    },
                    onDoubleTap = {
                        onDoubleTap.invoke()
                    }
                )
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // First Camera Preview
        AndroidView(
            modifier = Modifier
                .width(with(density) { width.toDp() })
                .height(with(density) { height.toDp() })
                .padding(end = with(density) { sep.toDp() }),
            factory = { ctx ->
                PreviewView(ctx)
            },
            update = { view ->
                val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
                    ProcessCameraProvider.getInstance(context)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview: Preview = Preview.Builder().build()

                    val cameraSelector: CameraSelector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()

                    preview.setSurfaceProvider(view.getSurfaceProvider())

                    val camera =
                    cameraProvider.bindToLifecycle(
                        context as LifecycleOwner,
                        cameraSelector,
                        preview
                    )

                    var pressState = false
                    onLongPress = {
                        onLongPress = {
                            pressState = !pressState
                            camera.cameraControl.enableTorch(pressState)
                        }
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )
        // Second Camera Preview (same setup as the first one)
        AndroidView(
            modifier = Modifier
                .width(with(density) { width.toDp() })
                .height(with(density) { height.toDp() })
                .padding(start = with(density) { sep.toDp() }),  // Ensure padding separates the views
            factory = { ctx ->
                PreviewView(ctx)
            },
            update = { view ->
                val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
                    ProcessCameraProvider.getInstance(context)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview: Preview = Preview.Builder().build()

                    val cameraSelector: CameraSelector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()

                    preview.setSurfaceProvider(view.surfaceProvider)
                    cameraProvider.bindToLifecycle(
                        context as LifecycleOwner,
                        cameraSelector,
                        preview
                    )
                }, ContextCompat.getMainExecutor(context))
            }
        )
    }
}