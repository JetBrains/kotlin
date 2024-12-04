/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlinx.cinterop.convert
import platform.posix.*

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun box(): String {
    // Just check the typealias is in scope.
    val sizet: size_t = 0.convert<size_t>()
    if (sizet.toString() == "0")
        return "OK"
    return "FAIL: $sizet"
}
