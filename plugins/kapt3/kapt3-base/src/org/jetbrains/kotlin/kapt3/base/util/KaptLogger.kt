/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.util

import java.io.PrintWriter

interface KaptLogger {
    val isVerbose: Boolean

    val infoWriter: PrintWriter
    val warnWriter: PrintWriter
    val errorWriter: PrintWriter

    fun info(message: String)
    fun warn(message: String)
    fun error(message: String)

    fun exception(e: Throwable)
}

inline fun KaptLogger.info(message: () -> String) {
    if (isVerbose) {
        info(message())
    }
}