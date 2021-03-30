/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.native.concurrent.*
import kotlin.native.Platform

@ThreadLocal
var x = Any()

fun main() {
    Platform.isMemoryLeakCheckerActive = true
    val worker = Worker.start()

    worker.execute(TransferMode.SAFE, {}) {
        println(x)  // Make sure x is initialized
    }.result

    worker.requestTermination().result
}
