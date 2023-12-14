/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// FILE: lib.kt
class X(val s: String)

val x = X("zzz")

// FILE: lib2.kt
@file:OptIn(FreezingIsDeprecated::class)
import kotlin.native.concurrent.*

class Z(val x: Int)

val z1 = Z(42)

val z2 = Z(x.s.length)

// FILE: main.kt
@file:OptIn(ObsoleteWorkersApi::class)
import kotlin.native.concurrent.*
import kotlin.test.*

val sb = StringBuilder()

fun foo() {
    val worker = Worker.start()
    worker.execute(TransferMode.SAFE, { -> }, {
        it -> sb.appendLine(z1.x)
    }).consume { }
}

fun box(): String {
    foo()
    sb.appendLine(z2.x)

    assertEquals("""
        42
        3

    """.trimIndent(), sb.toString())
    return "OK"
}
