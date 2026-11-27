package com.thikeiw.lolinfo.ui.championaudio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.thikeiw.lolinfo.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAudioPlayer @Inject constructor(@ApplicationContext private val context: Context) :
  AudioPlayer {

  private val handler = Handler(Looper.getMainLooper())
  private var mediaPlayer: MediaPlayer? = null
  private var listener: AudioPlayer.Listener? = null
  private var urls: List<String> = emptyList()
  private var currentUrlIndex = 0
  private var startPositionMs = 0L

  private val progressRunnable =
    object : Runnable {
      override fun run() {
        val player = mediaPlayer ?: return
        try {
          if (player.isPlaying) {
            listener?.onProgress(player.currentPosition.toLong(), player.duration.toLong().coerceAtLeast(0))
            handler.postDelayed(this, 250)
          }
        } catch (_: IllegalStateException) {
        }
      }
    }

  override fun setListener(listener: AudioPlayer.Listener?) {
    this.listener = listener
  }

  override fun play(urls: List<String>, startPositionMs: Long) {
    stopInternal()
    this.urls = urls.filter { it.isNotBlank() }
    this.currentUrlIndex = 0
    this.startPositionMs = startPositionMs.coerceAtLeast(0)
    playCurrent()
  }

  override fun pause() {
    val player = mediaPlayer ?: return
    try {
      if (player.isPlaying) {
        player.pause()
        handler.removeCallbacks(progressRunnable)
      }
    } catch (_: IllegalStateException) {
    }
  }

  override fun resume() {
    val player = mediaPlayer ?: return
    try {
      if (!player.isPlaying) {
        player.start()
        handler.post(progressRunnable)
      }
    } catch (_: IllegalStateException) {
    }
  }

  override fun seekTo(positionMs: Long) {
    val player = mediaPlayer ?: return
    try {
      player.seekTo(positionMs.coerceAtLeast(0).toInt())
    } catch (_: IllegalStateException) {
    }
  }

  override fun stop() {
    stopInternal()
  }

  override fun release() {
    stopInternal()
    listener = null
  }

  private fun playCurrent() {
    val url = urls.getOrNull(currentUrlIndex)
    if (url == null) {
      listener?.onError()
      return
    }

    val player = MediaPlayer()
    mediaPlayer = player
    try {
      player.setAudioAttributes(
        AudioAttributes.Builder()
          .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .build(),
      )
      player.setDataSource(context, Uri.parse(url), headers())
      player.setOnPreparedListener { prepared ->
        try {
          if (startPositionMs > 0) prepared.seekTo(startPositionMs.toInt())
          prepared.start()
          startPositionMs = 0L
          listener?.onPrepared(prepared.duration.toLong().coerceAtLeast(0))
          handler.post(progressRunnable)
        } catch (_: IllegalStateException) {
          tryNextOrError(prepared)
        }
      }
      player.setOnCompletionListener {
        handler.removeCallbacks(progressRunnable)
        listener?.onCompletion()
      }
      player.setOnErrorListener { failed, _, _ ->
        tryNextOrError(failed)
        true
      }
      player.prepareAsync()
    } catch (_: Exception) {
      tryNextOrError(player)
    }
  }

  private fun tryNextOrError(player: MediaPlayer?) {
    handler.removeCallbacks(progressRunnable)
    releasePlayer(player)
    currentUrlIndex++
    if (currentUrlIndex < urls.size) {
      playCurrent()
    } else {
      listener?.onError()
    }
  }

  private fun stopInternal() {
    handler.removeCallbacks(progressRunnable)
    releasePlayer(mediaPlayer)
    mediaPlayer = null
  }

  private fun releasePlayer(player: MediaPlayer?) {
    if (player == null) return
    try {
      player.reset()
    } catch (_: IllegalStateException) {
    }
    try {
      player.release()
    } catch (_: IllegalStateException) {
    }
  }

  private fun headers(): Map<String, String> =
    mapOf(
      "User-Agent" to "LoLSkinScope/${BuildConfig.VERSION_NAME} (Android)",
      "Accept" to "audio/*,*/*;q=0.9",
    )
}
