/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.MonoClock

@ExperimentalTime
internal class ResettableClockMark {
    private val startMark = MonoClock.markNow()
    private var lastMark = startMark

    fun elapsedSinceLast(): Duration = lastMark.elapsedNow().also { lastMark = lastMark.plus(it) }
    fun elapsedSinceStart(): Duration = startMark.elapsedNow()
}
