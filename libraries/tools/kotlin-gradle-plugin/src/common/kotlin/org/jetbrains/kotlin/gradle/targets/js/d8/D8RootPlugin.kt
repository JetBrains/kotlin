/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.d8

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// To be compatible with previous KGP version, we need to keep D8RootPlugin as deprecated.
@Deprecated(
    "This type is deprecated. Use D8Plugin instead. Scheduled for removal in Kotlin 2.3.",
    ReplaceWith("D8Plugin"),
    level = DeprecationLevel.ERROR
)
@OptIn(ExperimentalWasmDsl::class)
typealias D8RootPlugin = D8Plugin