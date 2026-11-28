/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.nativeBinaryOptions

enum class EraseKotlinLLVMDialect {
    /**
     * Never erase.
     */
    NEVER,

    /**
     * Erase right after running LLVM lowerings.
     */
    EARLY,

    /**
     * Erase after running all LLVM passes.
     */
    LATE,
}
