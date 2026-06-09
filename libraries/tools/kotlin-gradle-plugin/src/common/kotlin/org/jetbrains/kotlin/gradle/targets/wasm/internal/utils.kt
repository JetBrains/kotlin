/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.internal

import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.webTargetVariant

internal val KotlinJsIrTarget.isWasm: Boolean
    get() = webTargetVariant(
        jsVariant = false,
        wasmVariant = true,
    )

internal val KotlinJsIrCompilation.isWasm: Boolean
    get() = webTargetVariant(
        jsVariant = false,
        wasmVariant = true,
    )
