/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

@RequiresOptIn(
    message = "This API is intended to be used for Wasm Runtime DSL.",
    level = RequiresOptIn.Level.ERROR
)
annotation class ExperimentalWasmRuntimeDsl
