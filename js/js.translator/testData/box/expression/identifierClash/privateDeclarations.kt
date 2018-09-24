// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1286
open class A {
    private val `.` = "A"
    private val `;` = "B"

    private fun `@`() = "C"
    private fun `#`() = "D"

    fun foo() = `.` + `;` + `@`() + `#`()
}

fun box(): String {
    val x = A().foo()
    if (x != "ABCD") return "fail: $x"

    return "OK"
}