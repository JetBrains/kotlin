/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

enum class KotlinJsCompilerType {
    LEGACY,
    IR,
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

val KotlinJsCompilerType.lowerName
    get() = name.toLowerCase()

fun String.removeJsCompilerSuffix(compilerType: KotlinJsCompilerType): String {
    val truncatedString = removeSuffix(compilerType.lowerName)
    if (this != truncatedString) {
        return truncatedString
    }

    return removeSuffix(compilerType.lowerName.capitalize())
}