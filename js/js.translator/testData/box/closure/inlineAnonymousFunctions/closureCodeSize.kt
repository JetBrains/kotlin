// KT-51133

package foo

fun <T, R> myWith(t: T, f: (T) -> R) = f(t)

// CHECK_CONTAINS_NO_CALLS: noCapture except=myWith
// HAS_NO_CAPTURED_VARS: function=noCapture except=myWith;noCapture$lambda
fun noCapture() = myWith(42) { it }

// CHECK_CONTAINS_NO_CALLS: captureLocalVariableReadOnly except=myWith
// HAS_NO_CAPTURED_VARS: function=captureLocalVariableReadOnly except=myWith
fun captureLocalVariableReadOnly(a: Int) = myWith(1) { a + it }

// CHECK_CONTAINS_NO_CALLS: captureLocalVariableReadWrite except=myWith
// HAS_NO_CAPTURED_VARS: function=captureLocalVariableReadWrite except=myWith
fun captureLocalVariableReadWrite(): Int {
    var a = 41
    return myWith(1) {
        a += it
        a
    }
}

val thirteen = 13

// CHECK_CONTAINS_NO_CALLS: captureGlobalVariable except=myWith
// HAS_NO_CAPTURED_VARS: function=captureGlobalVariable except=myWith;captureGlobalVariable$lambda
fun captureGlobalVariable() = myWith(2) { thirteen * it }

class A(val i: Int) {

    fun captureClassField() = myWith(100) { it + i }

    inner class B(val j: Int) {

        inner class C(val k: Int) {
            fun captureInnerClassField() = myWith(200) { it + i + j + k }

            fun unusedLambda() {
                { i + j + k }
            }
        }
    }
}

fun A.captureClassFieldInExtension() = myWith(300) { it + i }

class D(val l: Int) {
    fun A.captureClassFieldInMemberExtension() = myWith(400) { it + i + l }

    fun captureClassFieldInMemberExtension(a: A) = a.captureClassFieldInMemberExtension()
}

fun unusedLambda(f: () -> Unit) {
    { f() }
}

fun box(): String {
    if (noCapture() != 42) return "fail noCapture()"
    if (captureLocalVariableReadOnly(41) != 42) return "fail captureLocalVariableReadOnly(41)"
    if (captureLocalVariableReadWrite() != 42) return "fail captureLocalVariableReadWrite()"
    if (captureGlobalVariable() != 26) return "fail captureGlobalVariable()"
    if (A(23).captureClassField() != 123) return "fail A(23).captureClassField()"
    if (A(300).B(400).C(500).captureInnerClassField() != 1400) return "A(300).B(400).C(500).captureInnerClassField()"
    if (A(12).captureClassFieldInExtension() != 312) return "A(12).captureClassFieldInExtension()"
    if (D(37).captureClassFieldInMemberExtension(A(100)) != 537) return "D(37).captureClassFieldInMemberExtension(A(100))"

    return "OK"
}
