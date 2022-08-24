// EXPECTED_REACHABLE_NODES: 1571
// KJS_WITH_FULL_RUNTIME
// IGNORE_FIR
// Because there is a bug inside FIR related to `enumValues` and `enumValueOf` resolution
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

    assertEquals(JsEnum.valueOf("Foo"), JsEnum.Foo)
    assertEquals(JsEnum.valueOf("Bar"), JsEnum.Bar)
    assertEquals(JsEnum.valueOf("Baz"), JsEnum.Baz)

    assertEquals(enumValueOf<JsEnum>("Foo"), JsEnum.Foo)
    assertEquals(enumValueOf<JsEnum>("Bar"), JsEnum.Bar)
    assertEquals(enumValueOf<JsEnum>("Baz"), JsEnum.Baz)


    assertEquals(enumValues<JsEnum>().asList(), JsEnum.values().asList())
    assertEquals(enumValues<JsEnum>().asList(), listOf(JsEnum.Foo, JsEnum.Bar, JsEnum.Baz))
    assertEquals(JsEnum.values().asList(), listOf(JsEnum.Foo, JsEnum.Bar, JsEnum.Baz))

    assertTrue(JsEnum.Foo == JsEnum.Foo)
    assertFalse(JsEnum.Foo == JsEnum.Bar)

    assertTrue(JsEnum.Foo.equals(JsEnum.Foo))
    assertFalse(JsEnum.Foo.equals(JsEnum.Bar))

    assertTrue(JsEnum.Foo.toString() == JsEnum.Foo.toString())
    assertFalse(JsEnum.Foo.toString() == JsEnum.Bar.toString())

    assertTrue(JsEnum.Foo.hashCode() == JsEnum.Foo.hashCode())
    assertFalse(JsEnum.Foo.hashCode() == JsEnum.Bar.hashCode())

    return "OK"
}