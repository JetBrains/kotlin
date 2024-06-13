/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

internal object WasmJsEnvironmentChecker : JsLikeEnvironmentChecker(
    KotlinToolingDiagnostics.WasmJsEnvironmentNotChosenExplicitly,
    { it.platformType == KotlinPlatformType.wasm && (it as KotlinJsIrTarget).wasmTargetType == KotlinWasmTargetType.JS },
    listOf(
        "browser()",
        "nodejs()",
        "d8"
    ),
    listOf(
        { it.browserNotConfigured() },
        { it.nodejsNotConfigured() },
        { it.d8NotConfigured() }
    )
)