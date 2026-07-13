// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true

// KT-86959: OptimizeCasts must not share a "complex" term (here, the result of bar())
// between different loop iterations. `previousBar` holds the previous iteration's bar()
// while `currentBar` holds the current one, so `previousBar && !currentBar` is not always false.

class A

var count = 0
fun bar(): Boolean {
    count += 1
    return count % 2 == 0
}

// CHECK-LABEL: define {{i1|zeroext i1}} @"kfun:#foo(kotlin.Any){}kotlin.Boolean"
fun foo(x: Any): Boolean {
    var previousBar = false
    var iterations = 0
    do {
        iterations++
        val currentBar = bar()
        if (previousBar && !currentBar) {
            // This branch is reachable (previousBar and currentBar are different bar() calls),
            // so `x is A` must be kept, not folded to a constant.
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
            return x is A
        }
        previousBar = currentBar
    } while (iterations < 10)
    return false
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {
    return if (foo(Any())) "fail" else "OK"
}
