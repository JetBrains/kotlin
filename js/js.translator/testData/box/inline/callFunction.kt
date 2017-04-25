// EXPECTED_REACHABLE_NODES: 495
// CHECK_CONTAINS_NO_CALLS: box except=equals;Baz_getInstance;callLocal;callLocalExtension
// CHECK_CONTAINS_NO_CALLS: callLocal
// CHECK_CONTAINS_NO_CALLS: callLocalExtension

object Foo {
    @JsName("call")
    inline fun call(a: Int) = "Foo.call($a)"
}

class Bar {
    @JsName("call")
    inline fun call(a: Int) = "Bar.call($a)"
}


inline fun call(a: Int) = "call($a)"

fun callLocal(a: Int): String {
    inline fun call(a: Int) = "callLocal($a)"
    return call(a)
}

object Baz

inline fun Baz.call(a: Int) = "Baz.call($a)"

class Boo

fun callLocalExtension(a: Int): String {
    inline fun Boo.call(a: Int) = "Boo.callLocal($a)"
    return Boo().call(a)
}

fun box(): String {
    var result = call(1)
    if (result != "call(1)") return "fail1: $result"

    result = Foo.call(2)
    if (result != "Foo.call(2)") return "fail2: $result"

    result = Bar().call(3)
    if (result != "Bar.call(3)") return "fail3: $result"

    result = Baz.call(4)
    if (result != "Baz.call(4)") return "fail4: $result"

    result = callLocal(5)
    if (result != "callLocal(5)") return "fail5: $result"

    result = callLocalExtension(6)
    if (result != "Boo.callLocal(6)") return "fail6: $result"

    return "OK"
}