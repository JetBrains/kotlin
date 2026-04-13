/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

/**
 * The different modes of the Kotlin compiler for compiling source code into an output artifact for the [KotlinPlatformType.js] platform.
 */
@Deprecated(
    "Kotlin/JS IR is the only supported compiler type. Remove compiler type selection from the DSL. Scheduled for removal in Kotlin 2.6.",
    level = DeprecationLevel.WARNING,
)
enum class KotlinJsCompilerType {
    /**
     * Represents the IR (Intermediate Representation) backend mode of the Kotlin compiler.
     */
    IR;

    /**
     * @suppress
     */
    companion object {
        const val jsCompilerProperty = "kotlin.js.compiler"

        @Suppress("DEPRECATION")
        fun byArgumentOrNull(argument: String): KotlinJsCompilerType? =
            values().firstOrNull { it.name.equals(argument, ignoreCase = true) }

        @Suppress("DEPRECATION")
        fun byArgument(argument: String): KotlinJsCompilerType =
            byArgumentOrNull(argument)
                ?: throw IllegalArgumentException(
                    "Unable to find $argument setting. Use [${values().toList().joinToString()}]"
                )
    }
}
