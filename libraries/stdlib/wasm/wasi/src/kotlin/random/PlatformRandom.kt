/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.random

import kotlin.wasm.WasiError
import kotlin.wasm.WasiErrorCode
import kotlin.wasm.ExperimentalWasmInterop
import kotlin.wasm.unsafe.withScopedMemoryAllocator

@OptIn(ExperimentalWasmInterop::class)
private fun wasiRandomGet(): Long {
    // TODO right random?
    return stdlib.wit.bindings.Random.getRandomU64().toLong()
}

internal actual fun defaultPlatformRandom(): Random = Random(wasiRandomGet())
