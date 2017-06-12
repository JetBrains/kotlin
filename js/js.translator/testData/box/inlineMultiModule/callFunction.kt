// EXPECTED_REACHABLE_NODES: 491
// MODULE: lib
// FILE: lib.kt

object Foo {
    @JsName("call")
    inline fun call(a: Int) = "Foo.call($a)"
}

class Bar {
    @JsName("call")
    inline fun call(a: Int) = "Bar.call($a)"
}


inline fun call(a: Int) = "call($a)"

object Baz

inline fun Baz.call(a: Int) = "Baz.call($a)"

// MODULE: main(lib)
// FILE: main.kt

// CHECK_CONTAINS_NO_CALLS: box except=equals

fun box(): String {
    var result = call(1)
    if (result != "call(1)") return "fail1: $result"

    result = Foo.call(2)
    if (result != "Foo.call(2)") return "fail2: $result"

    result = Bar().call(3)
    if (result != "Bar.call(3)") return "fail3: $result"

    result = Baz.call(4)
    if (result != "Baz.call(4)") return "fail4: $result"

    return "OK"
}