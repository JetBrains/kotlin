/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.videoplayer

import kotlinx.cinterop.*
import sdl.*

enum class PixelFormat {
    INVALID,
    RGB24,
    ARGB32
}

data class VideoOutput(val size: Dimensions, val pixelFormat: PixelFormat)

class SDLVideo : DisposableContainer() {
    private val displaySize: Dimensions
    private var window: SDLRendererWindow? = null

    init {
        disposable(
            create = { checkSDLError("Init", SDL_Init(SDL_INIT_EVERYTHING)) },
            dispose = { SDL_Quit() })
        displaySize = tryConstruct {
            memScoped {
                alloc<SDL_DisplayMode>().run {
                    checkSDLError("GetCurrentDisplayMode", SDL_GetCurrentDisplayMode(0, ptr.reinterpret()))
                    Dimensions(w, h)
                }
            }
        }
    }

    override fun dispose() {
        stop()
        super.dispose()
    }

    fun start(videoSize: Dimensions) {
        stop() // To free resources from previous playbacks.
        println("SDL Video: Playing output with ${videoSize.w} x ${videoSize.h} pixels")
        window = SDLRendererWindow((displaySize - videoSize) / 2, videoSize)
    }

    fun pixelFormat(): PixelFormat = window?.pixelFormat() ?: PixelFormat.INVALID

    fun nextFrame(frameData: CPointer<Uint8Var>, linesize: Int) =
        window?.nextFrame(frameData, linesize)

    fun stop() {
        window?.let {
            it.dispose()
            window = null
        }
    }
}

class SDLRendererWindow(windowPos: Dimensions, videoSize: Dimensions) : DisposableContainer() {
    private val window = sdlDisposable("CreateWindow",
        SDL_CreateWindow("VideoPlayer", windowPos.w, windowPos.h, videoSize.w, videoSize.h, SDL_WINDOW_SHOWN),
        ::SDL_DestroyWindow)
    private val renderer = sdlDisposable("CreateRenderer",
        SDL_CreateRenderer(window, -1, SDL_RENDERER_ACCELERATED or SDL_RENDERER_PRESENTVSYNC),
        ::SDL_DestroyRenderer)
    private val texture = sdlDisposable("CreateTexture",
        SDL_CreateTexture(renderer, SDL_GetWindowPixelFormat(window), 0, videoSize.w, videoSize.h),
        ::SDL_DestroyTexture)
    private val rect = sdlDisposable("calloc(SDL_Rect)",
        SDL_calloc(1, sizeOf<SDL_Rect>().convert()), ::SDL_free)
        .reinterpret<SDL_Rect>()

    init {
        rect.pointed.apply {
            x = 0
            y = 0
            w = videoSize.w
            h = videoSize.h
        }
    }

    fun pixelFormat(): PixelFormat = when (SDL_GetWindowPixelFormat(window)) {
        SDL_PIXELFORMAT_RGB24 -> PixelFormat.RGB24
        SDL_PIXELFORMAT_ARGB8888, SDL_PIXELFORMAT_RGB888 -> PixelFormat.ARGB32
        else -> {
            println("Pixel format ${SDL_GetWindowPixelFormat(window)} unknown")
            PixelFormat.INVALID
        }
    }

    fun nextFrame(frameData: CPointer<Uint8Var>, linesize: Int) {
        SDL_UpdateTexture(texture, rect, frameData, linesize)
        SDL_RenderClear(renderer)
        SDL_RenderCopy(renderer, texture, rect, rect)
        SDL_RenderPresent(renderer)
    }
}
