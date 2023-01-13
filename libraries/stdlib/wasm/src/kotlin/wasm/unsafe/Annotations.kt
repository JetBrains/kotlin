/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.unsafe

import kotlin.annotation.AnnotationTarget.*

/**
 * This annotation marks APIs for working with unmanaged WebAssembly linear memory.
 *
 * Any usage of a declaration annotated with `@UnsafeWasmMemoryApi` must be accepted either by
 * annotating that usage with the [OptIn] annotation, e.g. `@OptIn(UnsafeWasmMemoryApi::class)`,
 * or by using the compiler argument `-opt-in=kotlin.wasm.unsafe.UnsafeWasmMemoryApi`.
 */
@RequiresOptIn("Unsafe APIs to access to WebAssembly linear memory")
public annotation class UnsafeWasmMemoryApi