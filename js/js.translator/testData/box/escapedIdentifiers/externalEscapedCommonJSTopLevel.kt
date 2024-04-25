// IGNORE_BACKEND: JS
// MODULE_KIND: COMMON_JS
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

// FILE: lib.kt
@file:JsModule("lib")
package lib

external fun `@get something-invalid`(): String = definedExternally

external val `some+value`: Int = definedExternally

external object `+some+object%:` {
    val foo: String = definedExternally
}

// FILE: main.kt
import lib.`some+value`
import lib.`@get something-invalid`
import lib.`+some+object%:`

fun box(): String {
    assertEquals(42, `some+value`)
    assertEquals("%%++%%", `+some+object%:`.foo)
    assertEquals("something invalid", `@get something-invalid`())

    return "OK"
}