// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6
// TODO: Unmute when extension functions are supported in external declarations.
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: WASM

package foo

open class A(val v: String) {
    open fun m(i:Int, s:String): String = "A.m ${this.v} $i $s"
}

class B(v: String): A(v) {
    override fun m(i:Int, s:String): String = "B.m ${this.v} $i $s"
}

external fun bar(a: A, extLambda: A.(Int, String) -> String): String = definedExternally

fun A.topLevelExt(i:Int, s:String): String = "A::topLevelExt ${this.v} $i $s"

fun box(): String {
    val a = A("test")

    var r = bar(a) { i, s -> "${this.v} $i $s"}
    if (r != "test 4 boo") return r

    fun A.LocalExt(i:Int, s:String): String = "A::LocalExt ${this.v} $i $s"

    r = bar(a, fun A.(i, s) = (A::topLevelExt)(this, i, s))
    if (r != "A::topLevelExt test 4 boo") return r

    r = bar(a, fun A.(i, s) = (A::LocalExt)(this, i, s))
    if (r != "A::LocalExt test 4 boo") return r

    r = bar(a, fun A.(i, s) = (A::m)(this, i, s))
    if (r != "A.m test 4 boo") return r

    val b = B("test")
    r = bar(b, fun A.(i, s) = (A::m)(this, i, s))
    if (r != "B.m test 4 boo") return r

    return "OK"
}
