package com.kumanodormitory.pokke.ui.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.kumanodormitory.pokke.R

object SoundManager {

    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<Int, Int>()
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return

        val attr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        soundPool = SoundPool.Builder()
            .setAudioAttributes(attr)
            .setMaxStreams(3)
            .build()

        val pool = soundPool ?: return
        val resources = listOf(
            R.raw.cursor,
            R.raw.cursor2,
            R.raw.done,
            R.raw.error,
            R.raw.search,
            R.raw.transition,
            R.raw.scan1,
            R.raw.scan2,
            R.raw.scan3,
            R.raw.scaned,
            R.raw.faiz1,
            R.raw.faiz2,
            R.raw.faiz3,
            R.raw.faiz_pico,
            R.raw.faiz_complete,
            R.raw.tm2_pon001,
            R.raw.megaraba
        )

        for (resId in resources) {
            soundMap[resId] = pool.load(context, resId, 1)
        }

        initialized = true
    }

    fun play(resId: Int, pitch: Float = 1.0f) {
        val pool = soundPool ?: return
        val soundId = soundMap[resId] ?: return
        pool.play(soundId, 1.0f, 1.0f, 1, 0, pitch)
    }

    // Cursor sounds (旧アプリのパターンを踏襲)
    fun playCursorBlock(context: Context) {
        ensureInit(context)
        play(R.raw.cursor2, pitch = 1.0f)
    }

    fun playCursorRoom(context: Context) {
        ensureInit(context)
        play(R.raw.cursor2, pitch = 1.1f)
    }

    fun playCursorRyosei(context: Context) {
        ensureInit(context)
        play(R.raw.cursor2, pitch = 1.2f)
    }

    fun playDone(context: Context) {
        ensureInit(context)
        play(R.raw.done)
    }

    fun playError(context: Context) {
        ensureInit(context)
        play(R.raw.error)
    }

    fun playSearch(context: Context) {
        ensureInit(context)
        play(R.raw.search)
    }

    fun playTransition(context: Context) {
        ensureInit(context)
        play(R.raw.transition)
    }

    fun playFaizPico(context: Context) {
        ensureInit(context)
        play(R.raw.faiz_pico)
    }

    fun playFaizComplete(context: Context) {
        ensureInit(context)
        play(R.raw.faiz_complete)
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
        initialized = false
    }

    private fun ensureInit(context: Context) {
        if (!initialized) init(context.applicationContext)
    }
}
