// IGNORE_BACKEND: JS

fun jsNullUndefinedHashCode() {
    assertEquals(js("null").unsafeCast<Any>().hashCode(), js("null").unsafeCast<Any>().hashCode())
    assertEquals(js("undefined").unsafeCast<Any>().hashCode(), js("undefined").unsafeCast<Any>().hashCode())
    assertEquals(js("null").unsafeCast<Any>().hashCode(), js("undefined").unsafeCast<Any>().hashCode())
}

fun jsObjectHashCode() {
    val obj1 = js("{}").unsafeCast<Any>()
    val obj2 = js("{}").unsafeCast<Any>()
    val nullProtoObj1 = js("Object.create(null)").unsafeCast<Any>()
    val nullProtoObj2 = js("Object.create(null)").unsafeCast<Any>()
    val arr1 = js("[]").unsafeCast<Any>()
    val arr2 = js("[]").unsafeCast<Any>()

    assertEquals(obj1.hashCode(), obj1.hashCode())
    assertEquals(obj2.hashCode(), obj2.hashCode())
    assertEquals(nullProtoObj1.hashCode(), nullProtoObj1.hashCode())

    assertEquals(nullProtoObj2.hashCode(), nullProtoObj2.hashCode())
    assertEquals(arr1.hashCode(), arr1.hashCode())
    assertEquals(arr2.hashCode(), arr2.hashCode())

    assertNotEquals(obj1.hashCode(), obj2.hashCode())
    assertNotEquals(obj1.hashCode(), nullProtoObj1.hashCode())
    assertNotEquals(obj1.hashCode(), nullProtoObj2.hashCode())
    assertNotEquals(obj1.hashCode(), arr1.hashCode())
    assertNotEquals(obj1.hashCode(), arr2.hashCode())

    assertNotEquals(obj2.hashCode(), obj1.hashCode())
    assertNotEquals(obj2.hashCode(), nullProtoObj1.hashCode())
    assertNotEquals(obj2.hashCode(), nullProtoObj2.hashCode())
    assertNotEquals(obj2.hashCode(), arr1.hashCode())
    assertNotEquals(obj2.hashCode(), arr2.hashCode())

    assertNotEquals(nullProtoObj1.hashCode(), obj1.hashCode())
    assertNotEquals(nullProtoObj1.hashCode(), obj2.hashCode())
    assertNotEquals(nullProtoObj1.hashCode(), nullProtoObj2.hashCode())
    assertNotEquals(nullProtoObj1.hashCode(), arr1.hashCode())
    assertNotEquals(nullProtoObj1.hashCode(), arr2.hashCode())

    assertNotEquals(nullProtoObj2.hashCode(), obj1.hashCode())
    assertNotEquals(nullProtoObj2.hashCode(), obj2.hashCode())
    assertNotEquals(nullProtoObj2.hashCode(), nullProtoObj1.hashCode())
    assertNotEquals(nullProtoObj2.hashCode(), arr1.hashCode())
    assertNotEquals(nullProtoObj2.hashCode(), arr2.hashCode())

    assertNotEquals(arr1.hashCode(), obj1.hashCode())
    assertNotEquals(arr1.hashCode(), obj2.hashCode())
    assertNotEquals(arr1.hashCode(), nullProtoObj1.hashCode())
    assertNotEquals(arr1.hashCode(), nullProtoObj2.hashCode())
    assertNotEquals(arr1.hashCode(), arr2.hashCode())

    assertNotEquals(arr2.hashCode(), obj1.hashCode())
    assertNotEquals(arr2.hashCode(), obj2.hashCode())
    assertNotEquals(arr2.hashCode(), nullProtoObj1.hashCode())
    assertNotEquals(arr2.hashCode(), nullProtoObj2.hashCode())
    assertNotEquals(arr2.hashCode(), arr1.hashCode())
}

fun jsFunctionHashCode() {
    val fun1 = js("function() {}").unsafeCast<Any>()
    val fun2 = js("function() {}").unsafeCast<Any>()
    val randomObject = js("{}").unsafeCast<Any>()

    assertEquals(fun1.hashCode(), fun1.hashCode())
    assertEquals(fun2.hashCode(), fun2.hashCode())
    assertEquals(randomObject.hashCode(), randomObject.hashCode())

    assertNotEquals(fun1.hashCode(), fun2.hashCode())
    assertNotEquals(fun1.hashCode(), randomObject.hashCode())

    assertNotEquals(fun2.hashCode(), fun1.hashCode())
    assertNotEquals(fun2.hashCode(), randomObject.hashCode())

    assertNotEquals(randomObject.hashCode(), fun1.hashCode())
    assertNotEquals(randomObject.hashCode(), fun2.hashCode())
}

fun jsNumberHashCode() {
    assertEquals(js("4").unsafeCast<Any>().hashCode(), js("4").unsafeCast<Any>().hashCode())
    assertEquals(js("5").unsafeCast<Any>().hashCode(), js("5").unsafeCast<Any>().hashCode())
    assertEquals(js("-4").unsafeCast<Any>().hashCode(), js("-4").unsafeCast<Any>().hashCode())
    assertEquals(js("0").unsafeCast<Any>().hashCode(), js("-0").unsafeCast<Any>().hashCode())
    assertEquals(js("NaN").unsafeCast<Any>().hashCode(), js("NaN").unsafeCast<Any>().hashCode())
    assertEquals(js("Infinity").unsafeCast<Any>().hashCode(), js("Infinity").unsafeCast<Any>().hashCode())
    assertEquals(js("-Infinity").unsafeCast<Any>().hashCode(), js("-Infinity").unsafeCast<Any>().hashCode())

    assertNotEquals(js("-Infinity").unsafeCast<Any>().hashCode(), js("Infinity").unsafeCast<Any>().hashCode())

    assertNotEquals(js("4").unsafeCast<Any>().hashCode(), js("5").unsafeCast<Any>().hashCode())
    assertNotEquals(js("4").unsafeCast<Any>().hashCode(), js("-4").unsafeCast<Any>().hashCode())

    assertNotEquals(js("5").unsafeCast<Any>().hashCode(), js("4").unsafeCast<Any>().hashCode())
    assertNotEquals(js("5").unsafeCast<Any>().hashCode(), js("-4").unsafeCast<Any>().hashCode())

    assertNotEquals(js("-4").unsafeCast<Any>().hashCode(), js("4").unsafeCast<Any>().hashCode())
    assertNotEquals(js("-4").unsafeCast<Any>().hashCode(), js("5").unsafeCast<Any>().hashCode())
}

fun jsBooleanHashCode() {
    assertEquals(js("true").unsafeCast<Any>().hashCode(), js("true").unsafeCast<Any>().hashCode())
    assertEquals(js("false").unsafeCast<Any>().hashCode(), js("false").unsafeCast<Any>().hashCode())

    assertNotEquals(js("true").unsafeCast<Any>().hashCode(), js("false").unsafeCast<Any>().hashCode())
}

fun jsStringHashCode() {
    assertEquals(js("'Test'").unsafeCast<Any>().hashCode(), js("'Test'").unsafeCast<Any>().hashCode())
    assertEquals(js("'Test1'").unsafeCast<Any>().hashCode(), js("'Test1'").unsafeCast<Any>().hashCode())
    assertEquals(js("' Test'").unsafeCast<Any>().hashCode(), js("' Test'").unsafeCast<Any>().hashCode())

    assertNotEquals(js("'Test'").unsafeCast<Any>().hashCode(), js("' Test'").unsafeCast<Any>().hashCode())
    assertNotEquals(js("'Test'").unsafeCast<Any>().hashCode(), js("'Test1'").unsafeCast<Any>().hashCode())

    assertNotEquals(js("'Test1'").unsafeCast<Any>().hashCode(), js("'Test'").unsafeCast<Any>().hashCode())
    assertNotEquals(js("'Test1'").unsafeCast<Any>().hashCode(), js("' Test'").unsafeCast<Any>().hashCode())

    assertNotEquals(js("' Test'").unsafeCast<Any>().hashCode(), js("'Test'").unsafeCast<Any>().hashCode())
    assertNotEquals(js("' Test'").unsafeCast<Any>().hashCode(), js("'Test1'").unsafeCast<Any>().hashCode())
}

fun jsBigIntHashCode() {
    assertEquals(js("BigInt(4)").unsafeCast<Any>().hashCode(), js("BigInt(4)").unsafeCast<Any>().hashCode())
    assertEquals(js("BigInt(5)").unsafeCast<Any>().hashCode(), js("BigInt(5)").unsafeCast<Any>().hashCode())
    assertEquals(js("BigInt(-4)").unsafeCast<Any>().hashCode(), js("BigInt(-4)").unsafeCast<Any>().hashCode())

    assertNotEquals(js("BigInt(4)").unsafeCast<Any>().hashCode(), js("BigInt(5)").unsafeCast<Any>().hashCode())
    assertNotEquals(js("BigInt(4)").unsafeCast<Any>().hashCode(), js("BigInt(-4)").unsafeCast<Any>().hashCode())

    assertNotEquals(js("BigInt(5)").unsafeCast<Any>().hashCode(), js("BigInt(4)").unsafeCast<Any>().hashCode())
    assertNotEquals(js("BigInt(5)").unsafeCast<Any>().hashCode(), js("BigInt(-4)").unsafeCast<Any>().hashCode())

    assertNotEquals(js("BigInt(-4)").unsafeCast<Any>().hashCode(), js("BigInt(4)").unsafeCast<Any>().hashCode())
    assertNotEquals(js("BigInt(-4)").unsafeCast<Any>().hashCode(), js("BigInt(5)").unsafeCast<Any>().hashCode())
}

fun jsSymbolHashCode() {
    val symbol1 = js("Symbol()").unsafeCast<Any>()
    val symbol2 = js("Symbol()").unsafeCast<Any>()
    val symbol3 = js("Symbol.for('test')").unsafeCast<Any>()

    assertEquals(symbol1.hashCode(), symbol1.hashCode())
    assertEquals(symbol2.hashCode(), symbol2.hashCode())
    assertEquals(symbol3.hashCode(), symbol3.hashCode())
    assertEquals(symbol3.hashCode(), js("Symbol.for('test')").unsafeCast<Any>().hashCode())

    assertNotEquals(symbol1.hashCode(), symbol2.hashCode())
    assertNotEquals(symbol1.hashCode(), symbol3.hashCode())
    assertNotEquals(symbol1.hashCode(), js("Symbol.for('test')").unsafeCast<Any>().hashCode())

    assertNotEquals(symbol2.hashCode(), symbol1.hashCode())
    assertNotEquals(symbol2.hashCode(), symbol3.hashCode())
    assertNotEquals(symbol2.hashCode(), js("Symbol.for('test')").unsafeCast<Any>().hashCode())

    assertNotEquals(symbol3.hashCode(), symbol1.hashCode())
    assertNotEquals(symbol3.hashCode(), symbol2.hashCode())
}

fun box(): String {
    jsNullUndefinedHashCode()
    jsObjectHashCode()
    jsFunctionHashCode()
    jsNumberHashCode()
    jsBooleanHashCode()
    jsStringHashCode()
    jsBigIntHashCode()
    jsSymbolHashCode()

    return "OK"
}