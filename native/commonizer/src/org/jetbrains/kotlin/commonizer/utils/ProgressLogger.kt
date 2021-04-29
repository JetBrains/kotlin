/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.utils

import org.jetbrains.kotlin.commonizer.cli.CliLoggerAdapter
import org.jetbrains.kotlin.commonizer.CommonizerLogLevel
import org.jetbrains.kotlin.util.Logger

class ProgressLogger(
    private val wrapped: Logger = CliLoggerAdapter(CommonizerLogLevel.Info, 0),
    private val indent: Int = 0,
) : Logger by wrapped {
    private val clockMark = ResettableClockMark()
    private var finished = false

    private val prefix = "  ".repeat(indent) + " * "

    init {
        clockMark.reset()
        require(indent >= 0) { "Required indent >= 1" }
    }

    fun progress(message: String) {
        check(!finished)
        wrapped.log("$prefix$message in ${clockMark.elapsedSinceLast()}")
    }

    fun logTotal() {
        check(!finished)
        wrapped.log("TOTAL: ${clockMark.elapsedSinceStart()}")
        finished = true
    }

    fun fork(): ProgressLogger {
        return ProgressLogger(this, indent + 1)
    }
}
