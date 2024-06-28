// IGNORE_BACKEND: JS
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

package foo

external class A {
    val `@invalid @ val@`: Int = definedExternally
    var `--invalid-var`: String = definedExternally

    fun `get something$weird`(): String = definedExternally

    companion object {
        val `static val`: Int = definedExternally
        var `static var`: String = definedExternally

        fun `get 🦄`(): String = definedExternally
    }
}

fun box(): String {
    val a = A()

    assertEquals(23, a.`@invalid @ val@`)
    assertEquals("A: before", a.`--invalid-var`)
    assertEquals("something weird", a.`get something$weird`())

    a.`--invalid-var` = "A: after"
    assertEquals("A: after", a.`--invalid-var`)

    assertEquals(42, A.Companion.`static val`)
    assertEquals("Companion: before", A.Companion.`static var`)
    assertEquals("\uD83E\uDD84", A.Companion.`get 🦄`())

    A.`static var` = "Companion: after"

    assertEquals("Companion: after", A.Companion.`static var`)

    return "OK"
}