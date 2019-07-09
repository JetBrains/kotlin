/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.random

import kotlin.math.pow

internal actual fun defaultPlatformRandom(): Random =
    Random(js("(Math.random() * Math.pow(2, 32)) | 0").unsafeCast<Int>())


private val INV_2_26: Double = 2.0.pow(-26)
private val INV_2_53: Double = 2.0.pow(-53)
internal actual fun doubleFromParts(hi26: Int, low27: Int): Double =
    hi26 * INV_2_26 + low27 * INV_2_53