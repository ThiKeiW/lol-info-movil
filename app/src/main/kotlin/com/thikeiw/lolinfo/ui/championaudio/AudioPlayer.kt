package com.thikeiw.lolinfo.ui.championaudio

interface AudioPlayer {
  interface Listener {
    fun onPrepared(durationMs: Long)

    fun onProgress(positionMs: Long, durationMs: Long)

    fun onCompletion()

    fun onError()
  }

  fun setListener(listener: Listener?)

  fun play(urls: List<String>, startPositionMs: Long = 0)

  fun pause()

  fun resume()

  fun seekTo(positionMs: Long)

  fun stop()

  fun release()
}
