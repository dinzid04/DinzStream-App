package com.yuranime.dinz.ui.streaming

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.data.local.LocalAuthDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ActivePlaybackMetadata(
    val episodeId: String = "",
    val movieId: String = "",
    val streamUrl: String = "",
    val animeTitle: String = "",
    val episodeNumber: String = "1",
    val thumbnailUrl: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

/**
 * Pengelola singleton sesi ExoPlayer bersama agar transisi antara
 * StreamingScreen penuh, In-App Floating Mini-Player, dan Android Picture-in-Picture (PiP)
 * berlangsung mulus tanpa mengulang video dari awal.
 */
object StreamingPlayerHolder {
    private var player: ExoPlayer? = null
    private var appContext: Context? = null

    private val _metadata = MutableStateFlow(ActivePlaybackMetadata())
    val metadata: StateFlow<ActivePlaybackMetadata> = _metadata.asStateFlow()

    private val _isMiniPlayerVisible = MutableStateFlow(false)
    val isMiniPlayerVisible: StateFlow<Boolean> = _isMiniPlayerVisible.asStateFlow()

    private val _isInSystemPip = MutableStateFlow(false)
    val isInSystemPip: StateFlow<Boolean> = _isInSystemPip.asStateFlow()

    private val coreListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val p = player
            _metadata.update {
                it.copy(
                    isPlaying = isPlaying,
                    positionMs = p?.currentPosition?.coerceAtLeast(0L) ?: it.positionMs,
                    durationMs = p?.duration?.takeIf { d -> d > 0L } ?: it.durationMs
                )
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val p = player ?: return
            if (playbackState == Player.STATE_READY) {
                _metadata.update {
                    it.copy(
                        positionMs = p.currentPosition.coerceAtLeast(0L),
                        durationMs = p.duration.takeIf { d -> d > 0L } ?: it.durationMs
                    )
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun getOrCreatePlayer(context: Context): ExoPlayer {
        val ctx = context.applicationContext
        appContext = ctx
        player?.let { return it }

        return ExoPlayer.Builder(ctx)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { created ->
                created.repeatMode = Player.REPEAT_MODE_OFF
                created.addListener(coreListener)
                player = created
            }
    }

    fun getPlayerOrNull(): ExoPlayer? = player

    fun isAlreadyLoaded(episodeId: String, streamUrl: String): Boolean {
        val p = player ?: return false
        val current = _metadata.value
        return episodeId.isNotBlank() &&
            streamUrl.isNotBlank() &&
            current.episodeId == episodeId &&
            current.streamUrl == streamUrl &&
            p.mediaItemCount > 0 &&
            p.playbackState != Player.STATE_IDLE
    }

    fun updateActiveMedia(
        episodeId: String,
        movieId: String,
        streamUrl: String,
        animeTitle: String,
        episodeNumber: String,
        thumbnailUrl: String
    ) {
        _metadata.update {
            it.copy(
                episodeId = episodeId,
                movieId = movieId,
                streamUrl = streamUrl,
                animeTitle = animeTitle.ifBlank { it.animeTitle },
                episodeNumber = episodeNumber.ifBlank { it.episodeNumber },
                thumbnailUrl = thumbnailUrl.ifBlank { it.thumbnailUrl }
            )
        }
    }

    fun updatePlaybackProgress(positionMs: Long, durationMs: Long, isPlaying: Boolean) {
        _metadata.update {
            it.copy(
                positionMs = positionMs.coerceAtLeast(0L),
                durationMs = if (durationMs > 0L) durationMs else it.durationMs,
                isPlaying = isPlaying
            )
        }
    }

    fun isActivelyPlaying(): Boolean {
        val p = player ?: return false
        return p.mediaItemCount > 0 &&
            _metadata.value.streamUrl.isNotBlank() &&
            (p.isPlaying || p.playWhenReady) &&
            p.playbackState != Player.STATE_IDLE &&
            p.playbackState != Player.STATE_ENDED
    }

    fun enterMiniPlayer() {
        if (_metadata.value.episodeId.isNotBlank() && _metadata.value.streamUrl.isNotBlank()) {
            _isMiniPlayerVisible.value = true
        }
    }

    fun exitMiniPlayerKeepingPlayback() {
        _isMiniPlayerVisible.value = false
    }

    fun setSystemPipMode(inPip: Boolean) {
        _isInSystemPip.value = inPip
    }

    fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) {
            p.pause()
        } else {
            p.play()
        }
    }

    fun persistCurrentWatchProgress(context: Context? = appContext, markCompleted: Boolean = false) {
        val ctx = context?.applicationContext ?: appContext ?: return
        val meta = _metadata.value
        val p = player
        val posMs = p?.currentPosition?.coerceAtLeast(0L) ?: meta.positionMs
        val durMs = p?.duration?.takeIf { it > 0L } ?: meta.durationMs
        if (meta.movieId.isBlank() || meta.episodeId.isBlank()) return
        if (posMs <= 3000L && !markCompleted) return

        runCatching {
            val saved = LocalAuthDatabase(ctx).saveWatchProgress(
                movieId = meta.movieId,
                animeTitle = meta.animeTitle.ifBlank { "Anime" },
                episodeId = meta.episodeId,
                episodeTitle = "Episode ${meta.episodeNumber.ifBlank { "1" }}",
                posterUrl = meta.thumbnailUrl,
                positionMs = posMs,
                durationMs = durMs,
                episodeNumber = meta.episodeNumber.ifBlank { "1" },
                isCompleted = markCompleted
            )
            if (saved != null) {
                FirebaseManager.notifyWatchHistoryChanged()
                FirebaseManager.syncWatchHistoryItem(ctx, saved)
            }
        }
    }

    fun stopAndClear(context: Context? = appContext) {
        persistCurrentWatchProgress(context, markCompleted = false)
        _isMiniPlayerVisible.value = false
        _metadata.value = ActivePlaybackMetadata()
        player?.let { p ->
            try {
                p.stop()
                p.clearMediaItems()
            } catch (_: Throwable) {
            }
        }
    }

    fun releaseCompletely(context: Context? = appContext) {
        stopAndClear(context)
        player?.let { p ->
            try {
                p.removeListener(coreListener)
                p.release()
            } catch (_: Throwable) {
            }
        }
        player = null
    }
}
