// IGNORE_BACKEND: JS
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

package foo

interface IA {
    fun `run•invalid@test`(): Int
    fun `run@invalid@test`(): Int
    fun run_invalid_test(): Int
}

@JsExport()
class A : IA {
    override fun `run•invalid@test`(): Int = 41
    override fun `run@invalid@test`(): Int = 34
    override fun run_invalid_test(): Int = 23
}

class B : IA {
    override fun `run•invalid@test`(): Int = 42
    override fun `run@invalid@test`(): Int = 35
    override fun run_invalid_test(): Int = 24
}

fun box(): String {
    val a: IA = A()
    val b: IA = B()

    assertEquals(23, a.run_invalid_test())
    assertEquals(24, b.run_invalid_test())

    assertEquals(34, a.`run@invalid@test`())
    assertEquals(35, b.`run@invalid@test`())

    assertEquals(41, a.`run•invalid@test`())
    assertEquals(42, b.`run•invalid@test`())

    assertEquals("function", js("typeof a['run•invalid@test']"))
    assertEquals(41, js("a['run•invalid@test']()"))
    assertEquals(js("undefined"), js("b['run•invalid@test']"))

    assertEquals("function", js("typeof a['run@invalid@test']"))
    assertEquals(34, js("a['run@invalid@test']()"))
    assertEquals(js("undefined"), js("b['run@invalid@test']"))

    return "OK"
}