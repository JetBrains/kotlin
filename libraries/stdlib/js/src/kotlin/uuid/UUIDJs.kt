/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.uuid

// TODO: There is crypto.randomUUID() that requires secure context: https://developer.mozilla.org/en-US/docs/Web/API/Crypto/randomUUID
//   And only newer browser versions support it.
// TODO: crypto.getRandomValues() is supported from Node.js version 17.4.0 (Released 2022-01-18)
//   Might need to use crypto.randomFillSync() for Node.js: https://nodejs.org/api/crypto.html#cryptorandomfillsyncbuffer-offset-size
internal actual fun secureRandomUUID(): UUID {
    val randomBytes = ByteArray(16)
    js("crypto.getRandomValues(randomBytes)")
    return uuidFromRandomBytes(randomBytes)
}

