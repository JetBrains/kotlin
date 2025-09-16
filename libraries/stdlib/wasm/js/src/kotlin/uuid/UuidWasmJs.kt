/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmJsInterop::class)

package kotlin.uuid

private fun cryptoGetRandomValues(size: Int): JsAny =
    js("crypto.getRandomValues(new Int8Array(size))")

private fun get(array: JsAny, index: Int): Byte =
    js("array[index]")

internal actual fun secureRandomBytes(destination: ByteArray): Unit {
    val int8Array = cryptoGetRandomValues(destination.size)
    for (idx in destination.indices) {
        destination[idx] = get(int8Array, idx)
    }
}
