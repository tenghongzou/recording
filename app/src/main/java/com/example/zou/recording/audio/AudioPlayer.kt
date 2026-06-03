package com.example.zou.recording.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

/**
 * Encapsulates [MediaPlayer] for playing back recorded audio.
 *
 * Uses the [MediaPlayer.setDataSource] (Context, Uri) overload so both
 * file:// and content:// URIs can be played.
 */
class AudioPlayer(private val context: Context) {

    private var player: MediaPlayer? = null

    /** Whether a player exists and is currently playing. */
    val isPlaying: Boolean
        get() = try {
            player?.isPlaying == true
        } catch (e: IllegalStateException) {
            Log.w(TAG, "isPlaying queried in an invalid state", e)
            false
        }

    /**
     * Plays [uri] from the beginning, invoking [onCompletion] when playback finishes.
     */
    fun play(uri: Uri, onCompletion: () -> Unit) {
        val mediaPlayer = player ?: MediaPlayer().also { player = it }
        try {
            mediaPlayer.reset()
            mediaPlayer.setOnCompletionListener { onCompletion() }
            mediaPlayer.setDataSource(context, uri)
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play uri: $uri", e)
        }
    }

    /** Pauses playback. */
    fun pause() {
        try {
            player?.pause()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "pause() called in an invalid state", e)
        }
    }

    /** Resumes playback from where it was paused. */
    fun resume() {
        try {
            player?.start()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "resume() called in an invalid state", e)
        }
    }

    /** Stops playback and seeks back to the beginning. */
    fun stop() {
        try {
            player?.let {
                it.pause()
                it.seekTo(0)
            }
        } catch (e: IllegalStateException) {
            Log.w(TAG, "stop() called in an invalid state", e)
        }
    }

    /** Releases resources held by the underlying player. */
    fun release() {
        try {
            player?.release()
        } catch (e: Exception) {
            Log.w(TAG, "release() failed", e)
        } finally {
            player = null
        }
    }

    private companion object {
        private const val TAG = "AudioPlayer"
    }
}
