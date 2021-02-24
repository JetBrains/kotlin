/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import org.jetbrains.kotlin.util.Logger

class ProgressLogger(
    private val wrapped: Logger,
    startImmediately: Boolean = false
) : Logger by wrapped {
    private val clockMark = ResettableClockMark()
    private var finished = true

    init {
        if (startImmediately)
            reset()
    }

    fun reset() {
        clockMark.reset()
        finished = false
    }

    override fun log(message: String) {
        check(!finished)
        wrapped.log("* $message in ${clockMark.elapsedSinceLast()}")
    }

    fun logTotal() {
        check(!finished)
        wrapped.log("TOTAL: ${clockMark.elapsedSinceStart()}")
        finished = true
    }
}
