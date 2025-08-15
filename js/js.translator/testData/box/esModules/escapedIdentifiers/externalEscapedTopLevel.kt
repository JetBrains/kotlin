// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping
// ES_MODULES
// MODULE: main

// FILE: lib.mjs
function someFunction() { return "something invalid" }
let someValue = 42
let someObject = { foo: "%%++%%" }

export {
    someFunction as "@get something-invalid",
    someValue as "some+value",
    someObject as "+some+object%:"
}

// FILE: lib.kt
@file:JsModule("./lib.mjs")
package lib

external fun `@get something-invalid`(): String = definedExternally

external var `some+value`: Int = definedExternally

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