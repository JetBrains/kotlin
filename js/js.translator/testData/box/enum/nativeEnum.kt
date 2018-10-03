// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1285
package foo

external enum class JsEnum {
    Foo,
    Bar,
    Baz
}

// TODO uncomment when KT-5605 will be fixed
// fun JsEnum.extImplicit() = Foo

fun box(): String {
    assertEquals(1, JsEnum.Foo)
    assertEquals("BAR", JsEnum.Bar)
    assertEquals("OK", JsEnum.Baz.asDynamic().ok)

    return "OK"
}