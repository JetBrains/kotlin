/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.random

private fun initialSeed(): Int =
    js("((Math.random() * Math.pow(2, 32)) | 0)")

internal actual fun defaultPlatformRandom(): Random =
    Random(initialSeed())

internal actual fun doubleFromParts(hi26: Int, low27: Int): Double =
    (hi26.toLong().shl(27) + low27) / (1L shl 53).toDouble()
