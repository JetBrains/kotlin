/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.videoplayer

import kotlin.native.concurrent.Worker
import kotlinx.cinterop.*
import sdl.*
import platform.posix.memset
import platform.posix.memcpy

enum class SampleFormat {
    INVALID,
    S16
}

data class AudioOutput(val sampleRate: Int, val channels: Int, val sampleFormat: SampleFormat)

private fun SampleFormat.toSDLFormat(): SDL_AudioFormat? = when (this) {
    SampleFormat.S16 -> AUDIO_S16SYS.convert()
    SampleFormat.INVALID -> null
}

class SDLAudio(private val player: VideoPlayer) : DisposableContainer() {
    private var state = State.STOPPED

    fun start(audio: AudioOutput) {
        stop()
        val audioFormat = audio.sampleFormat.toSDLFormat() ?: return
        println("SDL Audio: Playing output with ${audio.channels} channels, ${audio.sampleRate} samples per second")

        memScoped {
            // TODO: better mechanisms to ensure we have same output format here and in resampler of the decoder.
            val spec = alloc<SDL_AudioSpec>().apply {
                freq = audio.sampleRate
                format = audioFormat
                channels = audio.channels.convert()
                silence = 0u
                samples = 4096u
                userdata = player.worker.asCPointer()
                callback = staticCFunction(::audioCallback)
            }
            val realSpec = alloc<SDL_AudioSpec>()
            if (SDL_OpenAudio(spec.ptr, realSpec.ptr) < 0)
                throwSDLError("SDL_OpenAudio")
            // TODO: ensure real spec matches what we asked for.
            state = State.PAUSED
            resume()
        }
    }

    fun pause() {
        state = state.transition(State.PLAYING, State.PAUSED) { SDL_PauseAudio(1) }
    }

    fun resume() {
        state = state.transition(State.PAUSED, State.PLAYING) { SDL_PauseAudio(0) }
    }

    fun stop() {
        pause()
        state = state.transition(State.PAUSED, State.STOPPED) { SDL_CloseAudio() }
    }
}

private fun audioCallback(userdata: COpaquePointer?, buffer: CPointer<Uint8Var>?, length: Int) {
    // This handler will be invoked in the audio thread, so reinit runtime.
    initRuntimeIfNeeded()
    val decoder = DecoderWorker(Worker.fromCPointer(userdata))
    var outPosition = 0
    while (outPosition < length) {
        val frame = decoder.nextAudioFrame(length - outPosition)
        if (frame != null) {
            val toCopy = minOf(length - outPosition, frame.size - frame.position)
            memcpy(buffer + outPosition, frame.buffer.pointed.data + frame.position, toCopy.convert())
            frame.unref()
            outPosition += toCopy
        } else {
            // println("Decoder returned nothing!")
            memset(buffer + outPosition, 0, (length - outPosition).convert())
            break
        }
    }
}
