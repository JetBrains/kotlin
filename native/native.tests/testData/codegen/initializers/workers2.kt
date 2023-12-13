/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// FILE: lib.kt
import kotlin.native.concurrent.*

class Z(val x: Int)

@ThreadLocal
val z = Z(42)

// FILE: main.kt
@file:OptIn(ObsoleteWorkersApi::class)
import kotlin.native.concurrent.*

fun main() {
    println(z.x)
    val worker = Worker.start()
    worker.execute(TransferMode.SAFE, { -> }, {
        it -> println(z.x)
    }).consume { }
}
