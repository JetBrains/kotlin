/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

public actual object MonoClock : Clock {
    override fun mark(initialElapsed: Duration): ClockMark {
        TODO("not implemented: base on high-res browser or node perf counter")
    }
}