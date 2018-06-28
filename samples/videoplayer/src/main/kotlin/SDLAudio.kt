/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    SampleFormat.S16 -> AUDIO_S16SYS.narrow()
    SampleFormat.INVALID -> null
}

class SDLAudio(val player: VideoPlayer) : DisposableContainer() {
    private val threadData = arena.alloc<IntVar>().ptr
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
                channels = audio.channels.narrow()
                silence = 0
                samples = 4096
                userdata = threadData
                callback = staticCFunction(::audioCallback)
            }
            threadData.pointed.value = player.workerId
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

// Only set in the audio thread
private var decoder: DecoderWorker? = null

private fun audioCallback(userdata: COpaquePointer?, buffer: CPointer<Uint8Var>?, length: Int) {
    // This handler will be invoked in the audio thread, so reinit runtime.
    konan.initRuntimeIfNeeded()
    val decoder = decoder ?:
        DecoderWorker(userdata!!.reinterpret<IntVar>().pointed.value).also { decoder = it }
    var outPosition = 0
    while (outPosition < length) {
        val frame = decoder.nextAudioFrame(length - outPosition)
        if (frame != null) {
            val toCopy = minOf(length - outPosition, frame.size - frame.position)
            memcpy(buffer + outPosition, frame.buffer.pointed.data + frame.position, toCopy.signExtend())
            frame.unref()
            outPosition += toCopy
        } else {
            // println("Decoder returned nothing!")
            memset(buffer + outPosition, 0, (length - outPosition).signExtend())
            break
        }
    }
}