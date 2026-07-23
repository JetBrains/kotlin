// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true

// KT-85403: reading a top-level val (a trivial getter) inside an `if (x is T)` branch dropped
// the branch's path predicate in CastsOptimization, so the control flow merge after the `if`
// wrongly concluded `x !is T`, folding subsequent `x is T` checks to false.

val b = 42

open class A
class A1 : A()

// CHECK-LABEL: define i32 @"kfun:#foo(A){}kotlin.Int"
fun foo(a: A): Int {
// CHECK-DEBUG: @IsSubtype{{.*}}@"kclass:A1"
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    if (a is A1) {
        val c = b
    }
    // The check below must be kept, not folded to false.
// CHECK-DEBUG: @IsSubtype{{.*}}@"kclass:A1"
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    return if (a is A1) 1 else 0
// CHECK-LABEL: epilogue:
}

sealed class B
class B1 : B()

// The original reproducer's shape: before the fix, the `is B1` check of the exhaustive `when`
// was folded to false, and the `when` threw NoWhenBranchMatchedException.
fun bar(x: B): Int {
    if (x is B1) {
        val c = b
    }
    return when (x) {
        is B1 -> 1
    }
}

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {
    if (foo(A1()) != 1) return "fail 1"
    if (foo(A()) != 0) return "fail 2"
    if (bar(B1()) != 1) return "fail 3"

    return "OK"
}
