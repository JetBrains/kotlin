// IGNORE_BACKEND: JS
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

package foo

@JsExport()
class A {
    class `$invalid inner` {}
}

class B {
    class `$invalid inner` {}
}

fun box(): String {
    // DCE preventing
    val b = B()

    val aCtor = A::class.js.asDynamic()
    val bCtor = B::class.js.asDynamic()

    assertEquals("function", typeOf(aCtor["\$invalid inner"]))
    assertEquals(js("undefined"), bCtor["\$invalid inner"])

    return "OK"
}

private fun typeOf(t: Any): String = js("typeof t")