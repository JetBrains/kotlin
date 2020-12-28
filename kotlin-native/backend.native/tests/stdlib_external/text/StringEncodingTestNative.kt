/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package test.text


@SharedImmutable
internal actual val surrogateCodePointDecoding: String = "\uFFFD".repeat(3)

@SharedImmutable
internal actual val surrogateCharEncoding: ByteArray = byteArrayOf(0xEF.toByte(), 0xBF.toByte(), 0xBD.toByte())
