// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true -Xdisable-phases=OptimizeCasts
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_EVERYWHERE

// KT-86949: ComputeTypesPass must not narrow a control flow merge point's type to a
// final class across loop iterations as if a node reached on one iteration is reached
// on every one. OptimizeCasts is disabled so that ComputeTypesPass is
// tested in isolation (it mishandles the same case, see KT-86948).

class A

fun bar() = true

// CHECK-LABEL: define {{i1|zeroext i1}} @"kfun:#foo(){}kotlin.Boolean"
fun foo(): Boolean {
    val a = A()
    var x: Any = a
    var count = 0
    var last = false
    do {
        count++
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG-NOT: call ptr @"kfun:kotlin.native.internal#downcast
// On the second iteration `x` is an Any(), so the `if` expression is not always an `A`;
// its type must not be rewritten to `A`, which would coerce the `Any()` to `A` with an unsafe downcast.
        last = (if (bar()) x else a) is A
        x = Any()
    } while (count < 2)
    return last
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {
    return if (foo()) "fail" else "OK"
}
