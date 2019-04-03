/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

public actual object MonoClock : LongReadingClock(), Clock { // TODO: interface should not be required here
    override fun reading(): Long = System.nanoTime()
    override fun toString(): String = "Clock(System.nanoTime())"
    override val unit: DurationUnit = DurationUnit.NANOSECONDS
}