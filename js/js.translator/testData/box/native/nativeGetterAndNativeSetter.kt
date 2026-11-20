// DONT_TARGET_EXACT_BACKEND: WASM_JS
// WASM_MUTE_REASON: UNSUPPORTED_JS_INTEROP
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

    @nativeGetter
    operator fun get(a: Int): Any? = definedExternally

    @nativeSetter
    operator fun set(a: Int, v: Any?): Unit = definedExternally

    @nativeGetter
    fun take(a: String): Any? = definedExternally

    @nativeSetter
    fun put(a: String, v: Any?): Unit = definedExternally
}

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

fun testOverloads(obj: JsObject, key: Int, oldValue: Any?, newValue: Any) {
    assertEquals(oldValue, obj[key])
    obj[key] = newValue
    assertEquals(newValue, obj[key])
    obj[key] = null
    assertEquals(null, obj[key])
}

fun testOverloads(obj: JsObject, key: String, oldValue: Any?, newValue: Any) {
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

    testOverloads(b, "foo", "boo", "moo")
    testOverloads(b, "bar", 35, 67)
    testOverloads(b, "baz", undefined, 34)
    testOverloads(b, "qoox", undefined, t)
    testOverloads(b, 0, "ok", "OK!")
    testOverloads(b, 1, 2, 3)
    testOverloads(b, 2, undefined, "HI")
    testOverloads(b, 5, undefined, t)

    return "OK"
}
