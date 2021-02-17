/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cli

import org.jetbrains.kotlin.util.Logger

fun Logger.toProgressLogger(): Logger {
    return ProgressLogger(this)
}

private class ProgressLogger(private val logger: Logger) : Logger by logger {
    override fun log(message: String) {
        logger.log("* $message")
    }

    override fun warning(message: String) {
        logger.log("* $message")
    }
}
