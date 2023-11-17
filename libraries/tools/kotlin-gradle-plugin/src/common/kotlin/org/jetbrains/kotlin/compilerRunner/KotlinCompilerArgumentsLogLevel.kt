/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

/**
 * Controls the log level to print used Kotlin compilation compiler arguments to output.
 */
internal enum class KotlinCompilerArgumentsLogLevel(val value: String) {
    ERROR("error"),
    WARNING("warning"),
    INFO("info"),
    DEBUG("debug");

    companion object {
        fun fromPropertyValue(value: String): KotlinCompilerArgumentsLogLevel =
            values().single { it.value == value }

        val DEFAULT = DEBUG
    }
}

