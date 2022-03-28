/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import kotlin.native.Retain

class C

fun f(): Any {
    return C()
}

fun g() = f()

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#h(kotlin.Boolean){}kotlin.Any"
@Retain
fun h(cond: Boolean): Any {
    // CHECK: Kotlin_mm_safePointFunctionPrologue
    // CHECK-NOT: Kotlin_mm_safePointFunctionPrologue
    // CHECK: br
    if (cond) {
        // CHECK: Kotlin_mm_safePointFunctionPrologue
        // CHECK-NOT: Kotlin_mm_safePointFunctionPrologue
        // CHECK: br
        return listOf(C(), C())
    } else {
        // CHECK: Kotlin_mm_safePointFunctionPrologue
        // CHECK-NOT: Kotlin_mm_safePointFunctionPrologue
        return listOf(C(), C(), C())
    }
// CHECK: }
}

// CHECK-LABEL: define void @"kfun:#main(){}"()
@Retain
fun main() {
    // CHECK: Kotlin_mm_safePointFunctionPrologue
    // CHECK-NOT: Kotlin_mm_safePointFunctionPrologue
    println(g())
    println(h(true))
// CHECK: }
}