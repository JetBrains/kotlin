/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
fun box(): String {
    memScoped {
        val bufferLength = 100L
        val buffer = allocArray<ByteVar>(bufferLength)
    }
    return "OK"
}
