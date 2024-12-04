/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

enum class KotlinJsIrOutputGranularity {
    WHOLE_PROGRAM,
    PER_MODULE,
    PER_FILE;

    companion object {
        fun byArgument(argument: String): KotlinJsIrOutputGranularity? =
            values()
                .firstOrNull { it.name.replace("_", "-").equals(argument, ignoreCase = true) }
    }
}

fun KotlinJsIrOutputGranularity.toCompilerArgument(): String? {
    return when (this) {
        KotlinJsIrOutputGranularity.PER_FILE -> PER_FILE
        KotlinJsIrOutputGranularity.PER_MODULE -> PER_MODULE
        KotlinJsIrOutputGranularity.WHOLE_PROGRAM -> null
    }
}