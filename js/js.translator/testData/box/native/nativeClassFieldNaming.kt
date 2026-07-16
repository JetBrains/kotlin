// DONT_TARGET_EXACT_BACKEND: WASM_JS
// WASM_MUTE_REASON: UNSUPPORTED_JS_INTEROP
// ONLY_IR_DCE

// FILE: main.kt

package foo

// External classes can have field  names colliding with generated subclass field names,
// so we check for the naming convention used at the point of writing this test (i.e. a_1)

// We expect specific js to ensure that the logic of field naming is the same.
// The point of the test is to ensure that B's b won't have the same name as A's a_1.
// This can also happen with subclass methods (just a instead of a_1, which is more dangerous)

external open class A(a_1: Int) {
    val a_1: Int
    val b: Int
}

class B(val minimizedField: Int) : A(minimizedField / 2) {
    fun minimizedMethod() = 42
}
class X(val x: Int)

// To stop the fields from being removed by dce
fun ensureUsed(x: Int) = x

external fun getJsPropertyNames(o: Any): String

fun box(): String {
    val b = B(10)
    val x = X(42)
    ensureUsed(x.x + b.minimizedField)

    val xProps = getJsPropertyNames(x)
    if (xProps != "f_1") return "Fail: unexpected js name minimization (have [${xProps}], expected [f_1])"

    val bProps = getJsPropertyNames(b)
    if (bProps == "a_1") return "Fail: B has fields [a_1]"

    try {
        b.minimizedMethod()
    } catch (e: dynamic) {
        // Can fail because A's constructor can shadow the name `b`
        return "Fail: ${e}"
    }

    if (b.a_1 != 5) return "Fail: parent class's a_1 was set from subclass, as seen from subclass (expected 5, actual ${b.a_1})"
    val a: A = js("b") // We cannot use 'as' here because it generates Exception classes, which change subsequent field names
    if (a.a_1 != 5) return "Fail: parent class's a_1 was set from subclass, as seen from parent class (expected 5, actual ${b.a_1})"

    return "OK"
}

// FILE: external.js

function getJsPropertyNames(o) {
    return Object.getOwnPropertyNames(o).join(", ");
}

function A(a) {
    this.a_1 = a;
    this.b = a;
}
