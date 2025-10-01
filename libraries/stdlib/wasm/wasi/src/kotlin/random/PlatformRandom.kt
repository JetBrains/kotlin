/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.random

internal fun randomLong(): Long {
    return random.nextLong()
}

internal fun randomBytes(size: Int): ByteArray {
    return random.nextBytes(size)
}

private val random = Random(42)

internal actual fun defaultPlatformRandom(): Random = Random(random.nextLong())
