/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.random

import kotlin.math.pow

internal actual fun defaultPlatformRandom(): Random =
    XorWowRandom((js("Math").random().unsafeCast<Double>() * (2.0.pow(32.0))).toInt())


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