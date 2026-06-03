package com.example.zou.recording.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * Encapsulates [MediaRecorder] for recording audio to an m4a (AAC) file.
 */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null

    /**
     * Starts recording audio into [output] (an m4a file).
     *
     * @throws IOException if [MediaRecorder.prepare] fails, so the caller can detect failure.
     */
    fun start(output: File) {
        // Make sure any previous session is cleaned up first.
        stop()

        val mediaRecorder = createMediaRecorder()
        recorder = mediaRecorder

        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setOutputFile(output.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start recording", e)
            // Clean up the half-configured recorder before surfacing the failure.
            stop()
            throw e
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to start recording: illegal state", e)
            stop()
            throw e
        }
    }

    /**
     * Stops recording and releases resources. Safe to call multiple times.
     */
    fun stop() {
        val mediaRecorder = recorder ?: return
        recorder = null
        try {
            mediaRecorder.stop()
        } catch (e: IllegalStateException) {
            // stop() throws if start() was never (successfully) called; ignore.
            Log.w(TAG, "stop() called in an invalid state", e)
        } catch (e: RuntimeException) {
            // MediaRecorder.stop() can throw RuntimeException if no valid data was recorded.
            Log.w(TAG, "stop() failed", e)
        } finally {
            try {
                mediaRecorder.reset()
                mediaRecorder.release()
            } catch (e: Exception) {
                Log.w(TAG, "release() failed", e)
            }
        }
    }

    /**
     * Returns the maximum amplitude sampled since the previous call (0 when not
     * recording or on error), for driving a live waveform.
     */
    fun maxAmplitude(): Int {
        return try {
            recorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            Log.w(TAG, "maxAmplitude() queried in an invalid state", e)
            0
        }
    }

    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    private companion object {
        private const val TAG = "AudioRecorder"
    }
}
