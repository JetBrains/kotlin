/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.videoplayer

import sdl.SDL_GetError
import kotlinx.cinterop.*

fun throwSDLError(name: String): Nothing =
    throw Error("SDL_$name Error: ${SDL_GetError()!!.toKString()}")

fun checkSDLError(name: String, result: Int) {
    if (result != 0) throwSDLError(name)
}
