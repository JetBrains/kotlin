package foo

import js.enumerable
import js.native

native
fun <T> _enumerate(o: T): T = noImpl

native
fun _findFirst<T>(o: Any): T = noImpl

enumerable
class Test() {
    val a: Int = 100
    val b: String = "s"
}

class P() {
    enumerable
    val a: Int = 100
    val b: String = "s"
}

fun box(): Boolean {
    val test = _enumerate(Test())
    val p = _enumerate(P())
    return (100 == test.a && "s" == test.b) && p.a == 100 && _findFirst<Int>(object {
        val test = 100
    }) == 100;
}