// KIND: STANDALONE
// MODULE: Generics
// FILE: generics.kt
class Foo(val i: Int)

fun <T> id(param: T): T {
    return param
}

abstract class Box<T>(open var t: T)
class DefaultBox<T>(t: T): Box<T>(t)
class IntBox(override var t: Int): Box<Int>(t)
class TripleBox(i: Int): Box<Box<Box<Foo>>>(DefaultBox(DefaultBox(Foo(i)))) {
    fun unwrap() = t.t.t.i
    fun set(newValue: Int) {
        t.t.t = Foo(newValue)
    }
}
