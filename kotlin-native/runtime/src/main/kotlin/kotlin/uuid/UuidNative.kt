/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.uuid

import kotlin.native.internal.GCUnsafeCall

@GCUnsafeCall("Kotlin_ByteArray_fillWithRandomBytes")
private external fun fillWithRandomBytes(byteArray: ByteArray, size: Int): Unit

@ExperimentalStdlibApi
internal actual fun secureRandomUuid(): UUID {
    val randomBytes = ByteArray(UUID.SIZE_BYTES)
    fillWithRandomBytes(randomBytes, randomBytes.size)
    return uuidFromRandomBytes(randomBytes)
}