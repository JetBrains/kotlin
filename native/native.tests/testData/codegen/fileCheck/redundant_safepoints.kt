// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: RemoveRedundantSafepoints

// This test checks:
// - there is only one safepoint per function
// - safepoint function is inlined in OPT mode, unless SMALLBINARY is needed (for ex, watchos_arm32)
// Might fail under -Xbinary=gc=stwms and -Xbinary=gc=noop. In this case, just add ignore clause.

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
    // CHECK-SMALLBINARY: {{call .*Kotlin_mm_safePointFunctionPrologue}}
    // CHECK-BIGBINARY-OPT: _ZN12_GLOBAL__N_115safePointActionE

    // CHECK-SMALLBINARY-NOT: _ZN12_GLOBAL__N_115safePointActionE
    // CHECK-BIGBINARY-OPT-NOT: {{call .*Kotlin_mm_safePointFunctionPrologue}}

    // CHECK-SMALLBINARY-NOT: {{call .*Kotlin_mm_safePointFunctionPrologue}}
    // CHECK-BIGBINARY-OPT-NOT: _ZN12_GLOBAL__N_115safePointActionE
    if (cond) {
        return listOf(C(), C())
    } else {
        return listOf(C(), C(), C())
    }
// CHECK-LABEL: ret
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
@Retain
fun box(): String {
    // CHECK-SMALLBINARY: {{call .*Kotlin_mm_safePointFunctionPrologue}}
    // CHECK-BIGBINARY-OPT: _ZN12_GLOBAL__N_115safePointActionE

    // CHECK-SMALLBINARY-NOT: _ZN12_GLOBAL__N_115safePointActionE
    // CHECK-BIGBINARY-OPT-NOT: {{call .*Kotlin_mm_safePointFunctionPrologue}}

    // CHECK-SMALLBINARY-NOT: {{call .*Kotlin_mm_safePointFunctionPrologue}}
    // CHECK-BIGBINARY-OPT-NOT: _ZN12_GLOBAL__N_115safePointActionE
    println(g())
    println(h(true))
    return "OK"
// CHECK-LABEL: ret
}
