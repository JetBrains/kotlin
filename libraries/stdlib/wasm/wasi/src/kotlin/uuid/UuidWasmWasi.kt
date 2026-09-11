/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.uuid

internal actual fun secureRandomBytes(destination: ByteArray): Unit {
    val bytes = stdlib.wit.bindings.Random.getRandomBytes(destination.size.toULong())
    for ([idx, elem] in bytes.withIndex())
        destination[idx] = elem.toByte()
}
