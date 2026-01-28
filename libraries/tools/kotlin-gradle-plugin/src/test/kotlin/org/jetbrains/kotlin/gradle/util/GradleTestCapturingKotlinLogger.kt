/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.jetbrains.kotlin.buildtools.api.KotlinLogger

class GradleTestCapturingKotlinLogger : KotlinLogger {
    val messages: List<String>
        field = mutableListOf<String>()

    val exceptions: List<Throwable>
        field = mutableListOf<Throwable>()

    override val isDebugEnabled: Boolean
        get() = true

    override fun error(msg: String, throwable: Throwable?) {
        messages.add(msg)
        exceptions.add(throwable ?: return)
    }

    override fun warn(msg: String, throwable: Throwable?) {
        messages.add(msg)
        exceptions.add(throwable ?: return)
    }

    override fun info(msg: String) {
        messages.add(msg)
    }

    override fun debug(msg: String) {
        messages.add(msg)
    }

    override fun lifecycle(msg: String) {
        messages.add(msg)
    }
}