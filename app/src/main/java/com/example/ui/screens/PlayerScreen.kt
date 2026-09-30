package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import com.example.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.EpgProgram
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.DangerRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    item: PlaylistItem,
    epgProgram: EpgProgram? = null,
    startPositionMs: Long = 0L,
    onProgressUpdate: ((PlaylistItem, Long, Long) -> Unit)? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(0L) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    var activeStreamUrl by remember(item.id) { mutableStateOf(item.streamUrl) }
    var isUsingBackupStream by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    val httpDataSourceFactory = remember {
        DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
    }

    val mediaCodecSelector = remember {
        MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
            val decoders = MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
            // Always prioritize certified AOSP software decoders (c2.android.*, omx.google.*, softwareOnly)
            // Virtualized hardware decoders (Cuttlefish/Goldfish/Ranchu) fail C2StreamBufferTypeSetting interface query with error 6.
            decoders.sortedWith(
                compareByDescending<MediaCodecInfo> {
                    it.softwareOnly || it.name.startsWith("c2.android.") || it.name.startsWith("omx.google.")
                }.thenBy { it.name }
            )
        }
    }

    val renderersFactory = remember {
        DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            setMediaCodecSelector(mediaCodecSelector)
        }
    }

    val exoPlayer = remember {
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(DefaultDataSource.Factory(context, httpDataSourceFactory))

        ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                playWhenReady = true
            }
    }

    // Load active stream item
    LaunchedEffect(activeStreamUrl) {
        errorMessage = null
        isBuffering = true
        try {
            val mediaItem = MediaItem.fromUri(activeStreamUrl)
            exoPlayer.setMediaItem(mediaItem)
            if (startPositionMs > 0L) {
                exoPlayer.seekTo(startPositionMs)
            }
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (e: Exception) {
            errorMessage = "Yayın başlatılamadı: ${e.message}"
            isBuffering = false
        }
    }

    // App lifecycle observer for safe pause/resume
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                    if (exoPlayer.duration > 0L) {
                        onProgressUpdate?.invoke(item, exoPlayer.currentPosition, exoPlayer.duration)
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isPlaying && errorMessage == null) {
                        exoPlayer.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Lock to landscape or allow rotation during playback session
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    totalDuration = exoPlayer.duration.coerceAtLeast(0L)
                    errorMessage = null
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                var isHttpError = false
                var httpCode = 0
                var curr: Throwable? = error
                while (curr != null) {
                    if (curr is HttpDataSource.InvalidResponseCodeException) {
                        isHttpError = true
                        httpCode = curr.responseCode
                        break
                    }
                    if (curr.message?.contains("403") == true) {
                        isHttpError = true
                        httpCode = 403
                        break
                    }
                    if (curr.message?.contains("404") == true) {
                        isHttpError = true
                        httpCode = 404
                        break
                    }
                    if (curr is HttpDataSource.HttpDataSourceException) {
                        isHttpError = true
                        break
                    }
                    curr = curr.cause
                }

                if (isHttpError && !isUsingBackupStream) {
                    isUsingBackupStream = true
                    val backupUrl = if (item.type == ItemType.LIVE_TV) {
                        "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                    } else {
                        "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
                    }
                    activeStreamUrl = backupUrl
                    val codeDesc = if (httpCode > 0) " (HTTP $httpCode)" else ""
                    errorMessage = "Yayın kaynağına ulaşılamadı$codeDesc. Otomatik olarak yedek akışa bağlanılıyor..."
                } else {
                    val rootMsg = error.cause?.message ?: error.message ?: "Bilinmeyen hata"
                    errorMessage = when {
                        httpCode == 403 -> "Yayın sunucusu erişimi reddetti (HTTP 403). Akış şifreli veya coğrafi engelli olabilir."
                        httpCode == 404 -> "Yayın akışı bulunamadı (HTTP 404). Akış yayından kaldırılmış olabilir."
                        rootMsg.contains("Unable to connect", ignoreCase = true) -> "Yayın sunucusuna bağlanılamadı. İnternet bağlantınızı kontrol edin."
                        else -> "Yayın akışı yüklenemedi: $rootMsg"
                    }
                    isBuffering = false
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            activity?.requestedOrientation = originalOrientation
            exoPlayer.removeListener(listener)
            try {
                if (exoPlayer.duration > 0L) {
                    onProgressUpdate?.invoke(item, exoPlayer.currentPosition, exoPlayer.duration)
                }
            } catch (_: Exception) {}
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    // Update position ticker and record progress every 10 seconds
    LaunchedEffect(isPlaying) {
        var lastSavedSec = 0L
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
            totalDuration = exoPlayer.duration.coerceAtLeast(0L)
            val currentSec = currentPosition / 1000L
            if (currentSec - lastSavedSec >= 10L && totalDuration > 0L) {
                lastSavedSec = currentSec
                onProgressUpdate?.invoke(item, currentPosition, totalDuration)
            }
            delay(500)
        }
    }

    // Auto-hide controls after 4 seconds
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        // Video View
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val view = LayoutInflater.from(ctx).inflate(R.layout.view_exo_player, null, false) as PlayerView
                view.player = exoPlayer
                view.useController = false
                view.resizeMode = resizeMode
                view.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                view
            },
            update = { playerView ->
                if (playerView.player != exoPlayer) {
                    playerView.player = exoPlayer
                }
                playerView.resizeMode = resizeMode
            }
        )

        // Loading spinner
        if (isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = AccentCyan,
                    modifier = Modifier.size(54.dp),
                    strokeWidth = 4.dp
                )
            }
        }

        // Error banner
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xDD000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = DangerRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2C324D))
                                .clickable {
                                    errorMessage = null
                                    exoPlayer.prepare()
                                    exoPlayer.play()
                                }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text("Yeniden Dene", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        if (!isUsingBackupStream) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AccentCyan)
                                    .clickable {
                                        errorMessage = null
                                        isUsingBackupStream = true
                                        val backupUrl = if (item.type == ItemType.LIVE_TV) {
                                            "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                                        } else {
                                            "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
                                        }
                                        activeStreamUrl = backupUrl
                                    }
                                    .padding(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Text("Yedek Yayına Geç", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x33FFFFFF))
                                .clickable { onBack() }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text("Geri Çık", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Animated Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xCC000000),
                                Color.Transparent,
                                Color(0xEE000000)
                            )
                        )
                    )
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x661E243A))
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (item.type == ItemType.LIVE_TV) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(DangerRed)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CANLI", color = DangerRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            if (epgProgram != null) {
                                Text(
                                    text = "${epgProgram.title} (${epgProgram.formattedTimeRange()})",
                                    color = AccentCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            } else {
                                Text(
                                    text = item.subtitleInfo.ifEmpty { item.category },
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Aspect ratio toggle
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x661E243A))
                            .clickable {
                                resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                } else {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "En-Boy Oranı",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Picture-in-picture button
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x661E243A))
                                .clickable {
                                    activity?.enterPictureInPictureMode()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPicture,
                                contentDescription = "PiP Modu",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Center Play / Pause & Skip
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(36.dp)
                ) {
                    if (item.type != ItemType.LIVE_TV) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0x661E243A))
                                .clickable {
                                    exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "10s Geri",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(AccentCyan)
                            .clickable {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                            .testTag("btn_player_play_pause"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Duraklat" else "Oynat",
                            tint = Color(0xFF04101A),
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    if (item.type != ItemType.LIVE_TV) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0x661E243A))
                                .clickable {
                                    exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "10s İleri",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Bottom Seekbar & Timestamps
                if (item.type != ItemType.LIVE_TV && totalDuration > 0) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatTime(currentPosition), color = Color.White, fontSize = 12.sp)
                            Text(formatTime(totalDuration), color = TextSecondary, fontSize = 12.sp)
                        }

                        Slider(
                            value = if (totalDuration > 0) (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) else 0f,
                            onValueChange = { frac ->
                                val target = (frac * totalDuration).toLong()
                                currentPosition = target
                                exoPlayer.seekTo(target)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = AccentCyan,
                                activeTrackColor = AccentCyan,
                                inactiveTrackColor = Color(0x66FFFFFF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
