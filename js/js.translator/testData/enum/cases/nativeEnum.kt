package foo

native
enum class JsEnum {
    Foo
    Bar
    Baz
}

// TODO uncomment when KT-5605 will be fixed
// fun JsEnum.extImplicit() = Foo

fun box(): String {
    assertEquals(1, JsEnum.Foo)
    assertEquals("BAR", JsEnum.Bar)
    assertEquals("OK", (JsEnum.Baz: dynamic).ok)

    return "OK"
}