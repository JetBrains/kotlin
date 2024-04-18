/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import java.security.SecureRandom

private val secureRandom by lazy { SecureRandom() }

internal actual fun secureRandomUUID(): UUID {
    val randomBytes = ByteArray(UUID.SIZE_BYTES)
    secureRandom.nextBytes(randomBytes)
    return uuidFromRandomBytes(randomBytes)
}