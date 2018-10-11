/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.ranges

@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline operator fun Float.rangeTo(that: Float): ClosedFloatingPointRange<Float> =
    this.toDouble().rangeTo(that.toDouble()).unsafeCast<ClosedFloatingPointRange<Float>>()
