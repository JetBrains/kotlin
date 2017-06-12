// EXPECTED_REACHABLE_NODES: 548
package foo

inline fun <reified T : Any> foo(): JsClass<T> {
    val T = 1
    return jsClass<T>()
}

fun box(): String {
    check(jsClass<A>(), foo<A>())
    check(jsClass<B>(), foo<B>())
    check(jsClass<O>(), foo<O>())
    check(jsClass<E>(), foo<E>())

    return "OK"
}
