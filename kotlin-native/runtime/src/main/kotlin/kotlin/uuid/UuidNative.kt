/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.uuid

import kotlin.native.internal.escapeAnalysis.Escapes
import kotlin.native.internal.GCUnsafeCall

@GCUnsafeCall("Kotlin_Uuid_getRandomBytes")
@Escapes.Nothing
private external fun getRandomBytes(byteArray: ByteArray, size: Int): Unit

internal actual fun secureRandomBytes(destination: ByteArray): Unit {
    getRandomBytes(destination, destination.size)
}
