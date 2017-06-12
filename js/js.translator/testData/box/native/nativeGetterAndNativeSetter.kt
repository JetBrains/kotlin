// EXPECTED_REACHABLE_NODES: 500
package foo

@JsName("Object")
external class JsObject {
    @nativeGetter
    operator fun get(a: String): Any? = definedExternally

    @nativeSetter
    operator fun set(a: String, v: Any?): Unit = definedExternally

    @nativeGetter
    fun take(a: Int): Any? = definedExternally

    @nativeSetter
    fun put(a: Int, v: Any?): Unit = definedExternally
}

@nativeGetter
operator fun JsObject.get(a: Int): Any? = definedExternally

@nativeSetter
operator fun JsObject.set(a: Int, v: Any?): Unit = definedExternally

@nativeGetter
fun JsObject.take(a: String): Any? = definedExternally

@nativeSetter
fun JsObject.put(a: String, v: Any?): Unit = definedExternally


object t{}

external fun getTestObject(): JsObject = definedExternally

fun test(obj: JsObject, key: String, oldValue: Any?, newValue: Any) {
    assertEquals(oldValue, obj[key])
    obj[key] = newValue
    assertEquals(newValue, obj[key])
    obj[key] = null
    assertEquals(null, obj[key])
}

fun test(obj: JsObject, key: Int, oldValue: Any?, newValue: Any) {
    assertEquals(oldValue, obj.take(key))
    obj.put(key, newValue)
    assertEquals(newValue, obj.take(key))
    obj.put(key, null)
    assertEquals(null, obj.take(key))
}

fun testExtensions(obj: JsObject, key: Int, oldValue: Any?, newValue: Any) {
    assertEquals(oldValue, obj[key])
    obj[key] = newValue
    assertEquals(newValue, obj[key])
    obj[key] = null
    assertEquals(null, obj[key])
}

fun testExtensions(obj: JsObject, key: String, oldValue: Any?, newValue: Any) {
    assertEquals(oldValue, obj.take(key))
    obj.put(key, newValue)
    assertEquals(newValue, obj.take(key))
    obj.put(key, null)
    assertEquals(null, obj.take(key))
}

fun box(): String {
    val a = getTestObject()

    test(a, "foo", "boo", "moo")
    test(a, "bar", 35, 67)
    test(a, "baz", undefined, 34)
    test(a, "qoox", undefined, t)
    test(a, 0, "ok", "OK!")
    test(a, 1, 2, 3)
    test(a, 2, undefined, "HI")
    test(a, 5, undefined, t)

    val b = getTestObject()

    testExtensions(b, "foo", "boo", "moo")
    testExtensions(b, "bar", 35, 67)
    testExtensions(b, "baz", undefined, 34)
    testExtensions(b, "qoox", undefined, t)
    testExtensions(b, 0, "ok", "OK!")
    testExtensions(b, 1, 2, 3)
    testExtensions(b, 2, undefined, "HI")
    testExtensions(b, 5, undefined, t)

    return "OK"
}