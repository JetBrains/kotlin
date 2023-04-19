/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:OptIn(ObsoleteWorkersApi::class)
package runtime.workers.enum_identity

import kotlin.test.*
import kotlin.native.concurrent.*

enum class A {
    A, B
}

data class Foo(val kind: A)

// Enums are shared between threads so identity should be kept.
@Test
fun runTest() {
    val result = Worker.start().execute(TransferMode.SAFE, { Foo(A.B) }, { input ->
        input.kind == A.B
    }).result
    println(result)
}
