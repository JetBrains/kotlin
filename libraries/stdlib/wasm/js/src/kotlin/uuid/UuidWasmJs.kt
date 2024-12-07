/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.uuid

private fun cryptoGetRandomValues(size: Int): JsAny =
    js("crypto.getRandomValues(new Int8Array(size))")

private fun get(array: JsAny, index: Int): Byte =
    js("array[index]")

@ExperimentalUuidApi
internal actual fun secureRandomUuid(): Uuid {
    val int8Array = cryptoGetRandomValues(Uuid.SIZE_BYTES)
    val randomBytes = ByteArray(Uuid.SIZE_BYTES) { get(int8Array, it) }
    return uuidFromRandomBytes(randomBytes)
}
