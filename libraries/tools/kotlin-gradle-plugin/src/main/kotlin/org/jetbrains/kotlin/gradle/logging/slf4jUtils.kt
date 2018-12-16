/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.logging

import org.slf4j.Logger

internal inline fun Logger.kotlinDebug(message: () -> String) {
    if (isDebugEnabled) {
        kotlinDebug(message())
    }
}

internal fun Logger.kotlinInfo(message: String) {
    this.info("[KOTLIN] $message")
}

internal fun Logger.kotlinDebug(message: String) {
    this.debug("[KOTLIN] $message")
}

internal fun Logger.kotlinWarn(message: String) {
    this.warn("[KOTLIN] $message")
}
