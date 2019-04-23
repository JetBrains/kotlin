/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.system.*

@SinceKotlin("1.3")
@ExperimentalTime
public actual object MonoClock : AbstractLongClock(unit = DurationUnit.NANOSECONDS), Clock { // TODO: interface should not be required here
    override fun read(): Long = getTimeNanos()
    override fun toString(): String = "Clock(nanoTime)"
}