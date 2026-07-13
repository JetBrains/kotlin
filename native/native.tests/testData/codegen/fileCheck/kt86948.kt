// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true -Xdisable-phases=ComputeTypes

// KT-86948: CastsOptimization must not merge a type check/cast result across loop
// iterations as if a node reached on one iteration is reached on every one.
// ComputeTypes is disabled so that CastsOptimization is tested in isolation
// (ComputeTypes mishandles the same case, see KT-86949).

class A

fun bar() = true

fun consume(b: Boolean) = b

// In all three tests below there is a similar bug: on the first loop iteration
// the argument resolves to a known variable (an `A`), but on the second one
// it does not, so the check is not statically known and must be kept.

// The type check is in a general expression position (the `visitTypeOperator` path).
// CHECK-LABEL: define {{i1|zeroext i1}} @"kfun:#testa(){}kotlin.Boolean"
fun testa(): Boolean {
    val a = A()
    var x: Any = a
    var count = 0
    var last = false
    do {
        count++
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
        last = consume((if (bar()) x else a) is A)
        x = Any()
    } while (count < 2)
    return last
// CHECK-LABEL: epilogue:
}

// The type check is assigned to a Boolean variable (the `buildBooleanPredicate` path).
// CHECK-LABEL: define {{i1|zeroext i1}} @"kfun:#testb(){}kotlin.Boolean"
fun testb(): Boolean {
    val a = A()
    var x: Any = a
    var count = 0
    var last = false
    do {
        count++
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
        last = (if (bar()) x else a) is A
        x = Any()
    } while (count < 2)
    return last
// CHECK-LABEL: epilogue:
}

// The safe cast is assigned to a nullable variable (the `buildNullablePredicate` path).
// CHECK-LABEL: define i32 @"kfun:#testc(){}kotlin.Int"
fun testc(): Int {
    val a = A()
    var x: Any = a
    var count = 0
    var last: A? = a
    do {
        count++
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
        last = (if (bar()) x else a) as? A
        x = Any()
    } while (count < 2)
    return if (last == null) 0 else 1
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {
    if (testa()) return "FAIL KT-86948 (general position): type check wrongly optimized"
    if (testb()) return "FAIL KT-86948 (boolean var): type check wrongly optimized"
    if (testc() != 0) return "FAIL KT-86948 (safe cast): cast wrongly optimized"

    return "OK"
}
