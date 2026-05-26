/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

internal enum class BtaToolchain {
    JVM,

    /** JS klib compilation stage (no `--includes` argument). */
    JS_COMPILATION,

    /** JS linking phase (has `--includes` argument). */
    JS_LINKING,

    /** Wasm klib compilation stage (no `--includes` argument). */
    WASM_COMPILATION,

    /** Wasm linking phase (has `--includes` argument). */
    WASM_LINKING,
}
