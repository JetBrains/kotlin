/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class Wrapper(x: Int)

// CHECK-LABEL: "kfun:#f(kotlin.Int;kotlin.String){}kotlin.String"
fun f(x: Int, s: String): String {
    // CHECK: _ZN6kotlin2mm14ThreadRegistry22currentThreadDataNode_E
    // CHECK-NOT: _ZN6kotlin2mm14ThreadRegistry22currentThreadDataNode_E
    if (x < 0) throw IllegalStateException()
    if (x > 0) return f(x - 1, s)
    val b = Wrapper(2)
    val a = listOf(x, x, Wrapper(1), 2, x)
    return buildString {
        for (i in a) { appendLine("$s i") }
    }
// CHECK: {{^}$}}
}

fun main() { println(f(10, "123456")) }
