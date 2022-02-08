/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

inline fun foo(x: () -> Unit): String {
    x()
    return "OK"
}

fun String.id(s: String = this, vararg xs: Int): String = s

// CHECK-LABEL: "kfun:#main(){}"
fun main() {
    // CHECK-LABEL: entry
    // CHECK-NOT: call %struct.ObjHeader* @AllocInstance
    // CHECK-NOT: alloca
    val x = foo("Fail"::id)
    println(x)
    // CHECK-LABEL: epilogue
}