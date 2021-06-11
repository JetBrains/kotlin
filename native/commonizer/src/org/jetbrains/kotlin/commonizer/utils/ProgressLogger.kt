/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.utils

import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.util.Logger

private const val ansiReset = "\u001B[0m"
private const val ansiTimeColor = "\u001B[36m"
private const val ansiTargetColor = "\u001B[32m"

internal inline fun <T> Logger?.progress(message: String, action: () -> T): T {
    val clock = ResettableClockMark()
    clock.reset()
    try {
        return action()
    } finally {
        this?.log("$message ${ansiTimeColor}in ${clock.elapsedSinceLast()}$ansiReset")
    }
}

internal inline fun <T> Logger?.progress(
    target: CommonizerTarget, message: String, action: () -> T
): T {
    return progress("[$ansiTargetColor$target$ansiReset]: $message", action)
}
