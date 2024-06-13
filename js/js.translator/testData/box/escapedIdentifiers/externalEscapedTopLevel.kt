// IGNORE_BACKEND: JS
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

package foo

external fun `@get something-invalid`(): String = definedExternally

external var `some+value`: Int
    get() = definedExternally
    set(a: Int) = definedExternally

external object `+some+object%:` {
    val foo: String = definedExternally
}

fun box(): String {
    assertEquals(42, `some+value`)
    assertEquals("%%++%%", `+some+object%:`.foo)
    assertEquals("something invalid", `@get something-invalid`())

    `some+value` = 43
    assertEquals(43, `some+value`)

    return "OK"
}