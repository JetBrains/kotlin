/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.abi

/**
 * Marks parts of the Kotlin Gradle plugin related to the Kotlin Application Binary Interface (ABI) validation tool.
 *
 * The marked DSL may be changed or removed as development continues.
 * Therefore, there are no compatibility guarantees.
 */
@RequiresOptIn(
    "The ABI Validation DSL is experimental and may be changed or removed in any future release.",
    RequiresOptIn.Level.ERROR
)
@MustBeDocumented
annotation class ExperimentalAbiValidation