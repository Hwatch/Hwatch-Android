package com.hwatch.app.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.material3.Player
import androidx.media3.ui.compose.material3.PlayerDefaults
import com.hwatch.app.R
import com.hwatch.app.util.MediaUrl
import kotlinx.coroutines.delay

/** Media3 原生 Compose 播放器页面。 */
@OptIn(markerClass = [UnstableApi::class, ExperimentalApi::class])
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    args: PlayerArgs,
    serverBaseUrl: String,
    onBack: () -> Unit,
) {
    val player by viewModel.player.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() }

    LaunchedEffect(args.videoId, args.displayTitle, serverBaseUrl) {
        val uri = MediaUrl.build(serverBaseUrl, args.videoId) ?: return@LaunchedEffect
        viewModel.start(uri, args.displayTitle)
    }

    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        // orientation configuration change 也会走 ON_STOP，此时不应暂停。
        if (PlayerLifecycle.shouldPauseOnStop(activity?.isChangingConfigurations == true)) {
            viewModel.pause()
        }
    }

    FullscreenSystemUiEffect(
        isFullscreen = uiState.isFullscreen,
        previousOrientation = uiState.previousOrientation,
        activity = activity,
        view = view,
    )

    val currentOrientation = activity?.requestedOrientation
        ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    val exitFullscreenOrBack: () -> Unit = {
        if (uiState.isFullscreen) {
            viewModel.toggleFullscreen(currentOrientation)
        } else {
            onBack()
        }
    }
    BackHandler(onBack = exitFullscreenOrBack)

    var controlsVisible by remember { mutableStateOf(true) }
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(CONTROLS_AUTO_HIDE_MS)
            controlsVisible = false
        }
    }

    var seekFeedback by remember { mutableStateOf<SeekFeedback?>(null) }
    var seekFeedbackKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(seekFeedbackKey) {
        if (seekFeedbackKey > 0) {
            delay(SEEK_FEEDBACK_MS)
            seekFeedback = null
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val exoPlayer = player
        if (exoPlayer == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Player(
                player = exoPlayer,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(exoPlayer) {
                        detectTapGestures(
                            onTap = { controlsVisible = !controlsVisible },
                            onDoubleTap = { offset ->
                                val forward = offset.x >= size.width / 2f
                                val delta = if (forward) PlayerSeek.STEP_MS else -PlayerSeek.STEP_MS
                                val target = PlayerSeek.seekTarget(
                                    positionMs = exoPlayer.currentPosition,
                                    durationMs = exoPlayer.duration,
                                    deltaMs = delta,
                                )
                                exoPlayer.seekTo(target)
                                seekFeedback = if (forward) SeekFeedback.FORWARD else SeekFeedback.BACK
                                seekFeedbackKey++
                            },
                        )
                    },
                showControls = controlsVisible,
                topControls = { _, showControls ->
                    PlayerTopControls(
                        title = args.displayTitle.ifBlank { args.videoId },
                        isFullscreen = uiState.isFullscreen,
                        showControls = showControls,
                        onBack = exitFullscreenOrBack,
                        onToggleFullscreen = { viewModel.toggleFullscreen(currentOrientation) },
                    )
                },
                centerControls = { currentPlayer, showControls ->
                    PlayerDefaults.CenterControls(
                        player = currentPlayer,
                        visible = showControls,
                        backSecondary = {},
                        forwardSecondary = {},
                    )
                },
                errorOverlay = { _ ->
                    PlayerErrorOverlay(error = uiState.error, onRetry = viewModel::retry)
                },
            )

            if (uiState.isBuffering && uiState.error == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).offset(y = (-56).dp),
                )
            }

            seekFeedback?.let { feedback ->
                SeekFeedbackOverlay(
                    feedback = feedback,
                    modifier = Modifier
                        .align(
                            if (feedback == SeekFeedback.FORWARD) {
                                Alignment.CenterEnd
                            } else {
                                Alignment.CenterStart
                            },
                        )
                        .padding(horizontal = 32.dp),
                )
            }
        }
    }
}

@OptIn(markerClass = [UnstableApi::class])
@Composable
private fun PlayerTopControls(
    title: String,
    isFullscreen: Boolean,
    showControls: Boolean,
    onBack: () -> Unit,
    onToggleFullscreen: () -> Unit,
) {
    PlayerDefaults.TopControls(
        player = null,
        visible = showControls,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent),
                ),
            ),
    ) { _ ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.player_back),
                    tint = Color.White,
                )
            }
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onToggleFullscreen) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                    contentDescription = stringResource(
                        if (isFullscreen) R.string.player_exit_fullscreen else R.string.player_fullscreen,
                    ),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun PlayerErrorOverlay(error: PlaybackErrorKind?, onRetry: () -> Unit) {
    if (error == null) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.player_error_title),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(playbackErrorRes(error)),
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.player_retry))
        }
    }
}

private enum class SeekFeedback { BACK, FORWARD }

@Composable
private fun SeekFeedbackOverlay(feedback: SeekFeedback, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(
                if (feedback == SeekFeedback.FORWARD) {
                    R.string.player_seek_forward
                } else {
                    R.string.player_seek_back
                },
            ),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun FullscreenSystemUiEffect(
    isFullscreen: Boolean,
    previousOrientation: Int,
    activity: Activity?,
    view: View,
) {
    val orientationApplied = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            orientationApplied.value = true
        } else if (orientationApplied.value) {
            activity?.requestedOrientation = previousOrientation
        }
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        if (isFullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            activity?.let { owner ->
                WindowCompat.getInsetsController(owner.window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

private fun playbackErrorRes(kind: PlaybackErrorKind): Int = when (kind) {
    PlaybackErrorKind.NETWORK -> R.string.player_error_network
    PlaybackErrorKind.NOT_FOUND -> R.string.player_error_not_found
    PlaybackErrorKind.SERVER -> R.string.player_error_server
    PlaybackErrorKind.UNSUPPORTED -> R.string.player_error_unsupported
    PlaybackErrorKind.DECODER -> R.string.player_error_decoder
    PlaybackErrorKind.UNKNOWN -> R.string.player_error_unknown
}

private const val CONTROLS_AUTO_HIDE_MS = 3000L
private const val SEEK_FEEDBACK_MS = 700L

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
