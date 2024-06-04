/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.uuid

import org.khronos.webgl.Int8Array
import org.khronos.webgl.get

@Suppress("ClassName")
private external object crypto {
    fun getRandomValues(bytes: Int8Array)
}

@ExperimentalStdlibApi
internal actual fun secureRandomUuid(): UUID {
    val jsRandomBytes = Int8Array(UUID.SIZE_BYTES)
    crypto.getRandomValues(jsRandomBytes)
    // Copy the JS-provided Int8Array into Kotlin ByteArray
    val randomBytes = ByteArray(UUID.SIZE_BYTES) { jsRandomBytes[it] }
    return uuidFromRandomBytes(randomBytes)
}
