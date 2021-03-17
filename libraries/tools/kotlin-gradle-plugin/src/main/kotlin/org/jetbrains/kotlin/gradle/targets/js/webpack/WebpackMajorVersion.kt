/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.webpack

enum class WebpackMajorVersion(
    val value: String
) {
    V4("4"),
    V5("5");

    companion object {
        val DEFAULT = V5

        const val webpackMajorVersion = "kotlin.js.webpack.major.version"

        const val warningMessage = "Default webpack version now is 5. Support of webpack 4 is deprecated."

        var webpackVersionWarning = false

        fun byArgumentOrNull(argument: String): WebpackMajorVersion? =
            when (argument) {
                "4" -> V4
                "5" -> V5
                else -> null
            }

        fun byArgument(argument: String): WebpackMajorVersion =
            byArgumentOrNull(argument)
                ?: throw IllegalArgumentException(
                    "This webpack major version is not supported. Use [${WebpackMajorVersion.values().toList().joinToString { it.value }}]"
                )

        fun <T> WebpackMajorVersion.choose(
            defaultValue: T,
            v4Value: T
        ): T =
            when (this) {
                V5 -> defaultValue
                V4 -> v4Value
            }
    }
}