// IGNORE_BACKEND: JS
// !LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

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

    assertEquals("function", js("typeof A['\$invalid inner']"))
    assertEquals(js("undefined"), js("B['\$invalid inner']"))

    return "OK"
}