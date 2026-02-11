/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.internal

import org.jetbrains.kotlin.platform.wasm.WasmTarget

internal fun WasmTarget?.supportsPerKlibCompilation(): Boolean = this == WasmTarget.JS