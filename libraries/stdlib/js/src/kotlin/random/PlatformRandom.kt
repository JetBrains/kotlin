/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.random

import kotlin.math.pow

internal actual fun defaultPlatformRandom(): Random =
    Random(js("(Math.random() * Math.pow(2, 32)) | 0").unsafeCast<Int>())


internal actual fun fastLog2(value: Int): Int {
    // TODO: not so fast, make faster
    var v = value
    var log = -1
    while (v != 0) {
        v = v.ushr(1)
        log++
    }
    return log
}

private val INV_2_26: Double = 2.0.pow(-26)
private val INV_2_53: Double = 2.0.pow(-53)
internal actual fun doubleFromParts(hi26: Int, low27: Int): Double =
    hi26 * INV_2_26 + low27 * INV_2_53