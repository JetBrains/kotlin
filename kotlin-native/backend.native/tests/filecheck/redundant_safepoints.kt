/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
import kotlin.native.Retain

class C

fun f(): Any {
    return C()
}

fun g() = f()

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#h(kotlin.Boolean){}kotlin.Any"
@Retain
fun h(cond: Boolean): Any {
    // CHECK: _ZN12_GLOBAL__N_115safePointActionE
    // CHECK-SMALLBINARY: {{call .*Kotlin_mm_safePointFunctionPrologue}}
    // CHECK-NOT: _ZN12_GLOBAL__N_115safePointActionE
    // CHECK-NOT: {{call .*Kotlin_mm_safePointFunctionPrologue}}
    // CHECK: br
    // CHECK-SMALLBINARY: br
    if (cond) {
        // CHECK-NOT: _ZN12_GLOBAL__N_115safePointActionE
        // CHECK-NOT: {{call .*Kotlin_mm_safePointFunctionPrologue}}
        // CHECK: br
        // CHECK-SMALLBINARY: br
        return listOf(C(), C())
    } else {
        // CHECK-NOT: _ZN12_GLOBAL__N_115safePointActionE
        // CHECK-NOT: {{call .*Kotlin_mm_safePointFunctionPrologue}}
        return listOf(C(), C(), C())
    }
// CHECK: }
}

// CHECK-LABEL: define void @"kfun:#main(){}"()
@Retain
fun main() {
    // CHECK: _ZN12_GLOBAL__N_115safePointActionE
    // CHECK-SMALLBINARY: {{call .*Kotlin_mm_safePointFunctionPrologue}}
    // CHECK-NOT: _ZN12_GLOBAL__N_115safePointActionE
    // CHECK-NOT: {{call .*Kotlin_mm_safePointFunctionPrologue}}
    println(g())
    println(h(true))
// CHECK: }
}
