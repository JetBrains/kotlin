/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.math.fdlibm

//#define __HI(x) *(1+(int*)&x)
internal fun __HI(x: Double): Int = (x.toRawBits() ushr 32).toInt()
internal fun __HIu(x: Double): UInt = (x.toRawBits() ushr 32).toUInt()

//#define __LO(x) *(int*)&x
internal fun __LO(x: Double): Int = (x.toRawBits() and 0xFFFFFFFF).toInt()
internal fun __LOu(x: Double): UInt = (x.toRawBits() and 0xFFFFFFFF).toUInt()

internal fun doubleSetWord(d: Double = 0.0, hi: Int = __HI(d), lo: Int = __LO(d)): Double =
    Double.fromBits((hi.toLong() shl 32) or (lo.toLong() and 0xFFFFFFFF))

internal fun UInt.negate(): UInt = inv() + 1U