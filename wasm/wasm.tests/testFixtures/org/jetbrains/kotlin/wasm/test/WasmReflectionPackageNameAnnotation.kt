/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.test.services.ReflectionPackageNameAnnotation

/**
 * `kotlin.wasm.internal.ReflectionPackageName`, which the Wasm backend takes into account when generating RTTI.
 *
 * Test runners that group several tests into one compilation are expected to register it in `TestServices`,
 * so that `BatchingPackageInserter` compensates the package renaming in the reflective information.
 */
val wasmReflectionPackageNameAnnotation: ReflectionPackageNameAnnotation =
    ReflectionPackageNameAnnotation(
        fqName = "kotlin.wasm.internal.ReflectionPackageName",
        requiredOptInMarkers = listOf("kotlin.wasm.internal.InternalForKotlinWasmTests"),
    )
