/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.binaryen

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec

@Deprecated(
    "Use 'org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec' instead. Scheduled for removal in Kotlin 2.6.",
    ReplaceWith(
        "BinaryenEnvSpec",
        "org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec"
    ),
    level = DeprecationLevel.WARNING
)
@OptIn(ExperimentalWasmDsl::class)
typealias BinaryenRootEnvSpec = BinaryenEnvSpec
