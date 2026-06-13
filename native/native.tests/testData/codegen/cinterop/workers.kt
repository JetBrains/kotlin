/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.3
// ^^^ KT-86026 TODO: Rework testdata to move functions/globals definitions from .def/.h into separate source files

// TARGET_BACKEND: NATIVE
// WITH_PLATFORM_LIBS
// MODULE: cinterop
// FILE: workers.def
---
#include <stdarg.h>

static int sum(int first, int second) {
    return first + second;
}

// MODULE: main(cinterop)
// FILE: main.kt

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlin.test.*
import kotlinx.cinterop.*
import kotlin.native.concurrent.*
import workers.*

fun box(): String {
    testWorker1()
    testWorker2()

    return "OK"
}

fun testWorker1() {
    val worker = Worker.start()
    worker.execute(TransferMode.SAFE, { -> sum(7, 8) }, {
            it -> it
    }).consume {
            result -> assertEquals(15, result)
    }
}

fun testWorker2() {
    val worker = Worker.start()
    worker.execute(TransferMode.SAFE, { -> 9 }, {
            it -> sum(it, it + 1)
    }).consume {
            result -> assertEquals(19, result)
    }
}
