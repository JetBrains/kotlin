/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

enum class KotlinIrJsGeneratedTSValidationStrategy {
    ERROR,
    IGNORE;

    companion object {
        fun byArgument(argument: String): KotlinIrJsGeneratedTSValidationStrategy? =
            values().firstOrNull { it.name.equals(argument, ignoreCase = true) }
    }
}