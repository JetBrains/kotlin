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
import konan.worker.WorkerId
import platform.posix.memset
import platform.posix.memcpy

class SDLAudio(val player: VideoPlayer) : SDLBase() {

    private var threadData: CPointer<IntVar>? = null

    override fun init() {
        threadData = nativeHeap.alloc<IntVar>().ptr
    }

    override fun deinit() {
        if (threadData != null) {
            nativeHeap.free(threadData!!)
            threadData = null
        }
    }

    fun start(sampleRate: Int, channels: Int) {
        println("Audio: $channels channels, $sampleRate samples per second")

        memScoped {
            // TODO: better mechanisms to ensure we have same output format here and in resampler of the decoder.
            val spec = alloc<SDL_AudioSpec>()
            spec.freq = 44100
            spec.format = AUDIO_S16SYS.narrow()
            spec.channels = 2.narrow()
            spec.silence = 0
            spec.samples = 4096
            spec.callback = staticCFunction {
                userdata, buffer, length ->
                // This handler will be invoked in the audio thread, so reinit runtime.
                konan.initRuntimeIfNeeded()

                if (decoder == null) {
                    val callbackData = userdata!!.reinterpret<IntVar>()
                    decoder = DecodeWorker(callbackData.pointed.value)
                }
                var outPosition = 0
                while (outPosition < length) {
                    val frame = decoder!!.nextAudioFrame(length - outPosition)
                    if (frame != null) {
                       val toCopy = min(length - outPosition, frame.size - frame.position)
                       memcpy(buffer + outPosition, frame.buffer.pointed.data + frame.position, toCopy.signExtend())
                       frame.unref()
                       outPosition += toCopy
                    } else {
                      println("Decoder returned nothing!")
                      memset(buffer + outPosition, 0, (length - outPosition).signExtend())
                      break
                    }
                }
            }
            threadData!!.pointed.value = player.decoder.workerId()
            spec.userdata = threadData
            val realSpec = alloc<SDL_AudioSpec>()
            if (SDL_OpenAudio(spec.ptr, realSpec.ptr) < 0)
                throw Error("SDL_OpenAudio: ${get_SDL_Error()}")
            // TODO: ensure real spec matches what we asked for.

            resume()
        }
    }

    fun pause() {
        SDL_PauseAudio(1)
    }

    fun resume() {
        SDL_PauseAudio(0)
    }

    fun stop() {
        pause()
        SDL_CloseAudio()

    }
}

// This global is only set in the audio thread.
var decoder: DecodeWorker? = null
