package foo

inline fun <reified T> foo(): JsClass<T> {
    val T = 1
    return jsClass<T>()
}

fun box(): String {
    assertEquals(jsClass<A>(), foo<A>())
    assertEquals(jsClass<B>(), foo<B>())
    assertEquals(jsClass<O>(), foo<O>())
    assertEquals(jsClass<E>(), foo<E>())

    return "OK"
}
