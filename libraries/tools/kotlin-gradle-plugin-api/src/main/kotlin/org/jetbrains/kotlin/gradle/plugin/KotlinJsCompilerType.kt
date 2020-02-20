/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Named
import java.io.Serializable

// For Gradle attributes
@Suppress("EnumEntryName")
enum class KotlinJsCompilerType : Named, Serializable {
    legacy,
    ir,
    both;

    override fun getName(): String =
        name.toLowerCase()

    override fun toString(): String =
        getName()

    companion object {
        const val jsCompilerProperty = "kotlin.js.compiler"

        fun byArgument(argument: String): KotlinJsCompilerType? =
            values().firstOrNull { it.name.equals(argument, ignoreCase = true) }
    }
}

fun String.removeJsCompilerSuffix(compilerType: KotlinJsCompilerType): String {
    val truncatedString = removeSuffix(compilerType.name)
    if (this != truncatedString) {
        return truncatedString
    }

    return removeSuffix(compilerType.name.capitalize())
}