// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true

// KT-88316: two nullable local variables assigned to each other across nested loops made
// CastsOptimization's variable alias map cyclic (p -> q -> p): on a repeated loop iteration
// a variable read whose alias had changed returned the variable itself, even though that
// variable was aliased, and the assignment stored it as an alias. buildNullablePredicate
// follows alias chains transitively, so it recursed until the stack overflowed.

// CHECK-LABEL: define i32 @"kfun:#foo(kotlin.String?){}kotlin.Int"
fun foo(a: String?): Int {
    var count = 0
    var p = a
    while (p != null) {
        var q: String? = p
        while (q != null) {
            count++
            q = null
        }
        p = q
    }
    return count
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {
    if (foo(null) != 0) return "fail 1"
    if (foo("x") != 1) return "fail 2"

    return "OK"
}
