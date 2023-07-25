/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import java.util.*

enum class KotlinJsCompilerType {
    @Deprecated("Legacy compiler is deprecated. Migrate your project to the new IR-based compiler")
    LEGACY,

    IR,

    @Deprecated("Legacy compiler is deprecated. Migrate your project to the new IR-based compiler")
    BOTH;

    companion object {
        const val jsCompilerProperty = "kotlin.js.compiler"

        fun byArgumentOrNull(argument: String): KotlinJsCompilerType? =
            values().firstOrNull { it.name.equals(argument, ignoreCase = true) }

        fun byArgument(argument: String): KotlinJsCompilerType =
            byArgumentOrNull(argument)
                ?: throw IllegalArgumentException(
                    "Unable to find $argument setting. Use [${values().toList().joinToString()}]"
                )
    }
}

@Deprecated("This method is planned to be removed")
val KotlinJsCompilerType.lowerName
    get() = name.toLowerCase(Locale.ENGLISH)

@Suppress("DEPRECATION")
@Deprecated("This method is planned to be removed")
fun String.removeJsCompilerSuffix(compilerType: KotlinJsCompilerType): String {
    val truncatedString = removeSuffix(compilerType.lowerName)
    if (this != truncatedString) {
        return truncatedString
    }

    return removeSuffix(compilerType.lowerName.capitalize(Locale.ENGLISH))
}