/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.nativeBinaryOptions

/**
 * When cinterop emits the Kotlin function for a C function, it adds instructions for the compiler
 * on how to generate a call to this function.
 *
 * Such an instruction is an annotation that can be `@CCall(id)` or `@CCall.Direct(name)`.
 * cinterop has a flag that controls which to emit, `-Xccall-mode direct|indirect|both`.
 *
 * Additionally, the compiler can also choose which generation approach to use,
 * which is controlled by this enum, [CCallMode].
 *
 * See [KT-79751](https://youtrack.jetbrains.com/issue/KT-79751) for more details.
 */
enum class CCallMode {
    /**
     * Use only `@CCall`, ignore `@CCall.Direct`.
     * If a called function has only `@CCall.Direct`, the compilation fails with an error.
     */
    Indirect,

    /**
     * Use `@CCall` by default. If not available, fall back to `@CCall.Direct`.
     */
    IndirectOrDirect,

    /**
     * Use `@CCall.Direct` by default. If not available, fall back to `@CCall`.
     */
    DirectOrIndirect,

    /**
     * Use only `@CCall.Direct`, ignore `@CCall`.
     * If the function has only `@CCall`, the compilation fails with an error.
     */
    Direct,
}
