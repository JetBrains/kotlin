// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1224
interface A {
    fun foo(): String
}

class B : A {
    override fun foo(): String = "OK"
}

fun box(): String {
    val b = B::class.js
    val c = js("""
    function C() {
        b.call(this);
    };
    C.prototype = Object.create(b.prototype);
    C.prototype.constructor = C;
    new C();
    """)

    if (c !is B) return "fail: c !is B"
    if (c !is A) return "fail: c !is A"

    return "OK"
}
