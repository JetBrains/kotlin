/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.videoplayer

data class Dimensions(val w: Int, val h: Int) {
    operator fun minus(other: Dimensions) = Dimensions(w - other.w, h - other.h)
    operator fun div(b: Int) = Dimensions(w / b, h / b)
}
