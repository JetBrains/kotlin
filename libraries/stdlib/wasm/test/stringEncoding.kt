/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

// TODO: Fix this once we implement kotlin.text
internal actual val surrogateCodePointDecoding: String = "�"

internal actual val surrogateCharEncoding: ByteArray = byteArrayOf(0x3F)
