/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

enum class KotlinJsIrOutputGranularity {
    WHOLE_PROGRAM,
    PER_MODULE;

    companion object {
        fun byArgument(argument: String): KotlinJsIrOutputGranularity? =
            values()
                .firstOrNull { it.name.replace("_", "-").equals(argument, ignoreCase = true) }
    }
}

fun KotlinJsIrOutputGranularity.toCompilerArgument(): String {
    val perModule = this == KotlinJsIrOutputGranularity.PER_MODULE
    return "$PER_MODULE=$perModule"
}