/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import java.security.SecureRandom

private val secureRandom by lazy { SecureRandom() }

public actual fun secureRandomUUID(): UUID {
    val randomBytes = ByteArray(16)
    secureRandom.nextBytes(randomBytes)
    randomBytes[6] = (randomBytes[6].toInt() and 0x0f).toByte() /* clear version        */
    randomBytes[6] = (randomBytes[6].toInt() or 0x40).toByte()  /* set to version 4     */
    randomBytes[8] = (randomBytes[8].toInt() and 0x3f).toByte() /* clear variant        */
    randomBytes[8] = (randomBytes[8].toInt() or 0x80).toByte()  /* set to IETF variant  */
    return UUID(randomBytes)
}