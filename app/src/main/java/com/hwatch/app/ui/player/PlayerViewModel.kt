package com.hwatch.app.ui.player

import android.content.pm.ActivityInfo
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.hwatch.app.di.PlayerFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PlayerUiState(
    val isBuffering: Boolean = false,
    val isPlaying: Boolean = false,
    val error: PlaybackErrorKind? = null,
    val isFullscreen: Boolean = false,
    val previousOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
)

/**
 * 持有 ExoPlayer 的生命周期所有权。ViewModel 在 configuration change 时保留，
 * 因此旋转不会重建播放会话；只有真正离开 Player destination 时 `onCleared()` 才 release。
 */
class PlayerViewModel(
    private val playerFactory: PlayerFactory,
) : ViewModel() {

    private val _player = MutableStateFlow<ExoPlayer?>(null)
    val player: StateFlow<ExoPlayer?> = _player.asStateFlow()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentUri: String? = null

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _uiState.update { it.copy(isBuffering = playbackState == Player.STATE_BUFFERING) }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlayerError(error: PlaybackException) {
            _uiState.update { it.copy(error = PlaybackErrorMapper.map(error), isBuffering = false) }
        }
    }

    /** 幂等：相同 uri 且已创建 player 时不重复 setMediaItem/prepare（防旋转重复 prepare）。 */
    fun start(uri: String, title: String) {
        if (currentUri == uri && _player.value != null) return
        val exoPlayer = _player.value ?: playerFactory.create().also { created ->
            created.addListener(listener)
            _player.value = created
        }
        currentUri = uri
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title.ifBlank { uri }).build())
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        _uiState.update { it.copy(error = null) }
    }

    /** 重试：复用同一 ExoPlayer 与 MediaItem，不重新导航、不重建 ViewModel。 */
    fun retry() {
        val exoPlayer = _player.value ?: return
        _uiState.update { it.copy(error = null) }
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun pause() {
        _player.value?.pause()
    }

    fun toggleFullscreen(currentOrientation: Int) {
        _uiState.update { state ->
            if (state.isFullscreen) {
                state.copy(isFullscreen = false)
            } else {
                state.copy(isFullscreen = true, previousOrientation = currentOrientation)
            }
        }
    }

    override fun onCleared() {
        _player.value?.removeListener(listener)
        _player.value?.release()
        _player.value = null
        currentUri = null
    }
}
