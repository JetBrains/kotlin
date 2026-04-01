// TARGET_BACKEND: JS_IR_ES6

interface A {
    fun foo(): String
}

class B : A {
    override fun foo(): String = "OK"
}

fun box(): String {
    val b = B::class.js
    val c = js("""(function() {
    class C extends b {}
    return new C();
    })()""")

    if (c !is B) return "fail: c !is B"
    if (c !is A) return "fail: c !is A"

    return "OK"
}
