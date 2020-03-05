/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

@SinceKotlin("1.3")
@ExperimentalTime
internal actual object MonotonicTimeSource : AbstractLongTimeSource(unit = DurationUnit.NANOSECONDS), TimeSource { // TODO: interface should not be required here
    override fun read(): Long = System.nanoTime()
    override fun toString(): String = "TimeSource(System.nanoTime())"
}