// EXPECTED_REACHABLE_NODES: 498
private fun `+`(a: Int, b: Int) = a + b

@JsName("minus")
fun `-`(a: Int, b: Int) = a - b

fun test1(): Int {
    val ` x ` = 23
    val `*` = 3
    return `-`(` x `, `*`)
}

fun test2(`p 1`: Int, `.p 2`: Int) = `+`(`p 1`, `.p 2`)

fun test3(): String {
    val `#` = "K"
    class ` `(private val `::`: String) {
        private fun `@`() = `::` + `#`

        operator fun invoke() = `@`()
    }
    return ` `("O")()
}

fun test4(): String {
    val `()` = "OK"
    fun `[]`() = `()`
    return `[]`()
}

fun box(): String {
    if (test1() != 20) return "fail1"
    if (test2(10, 13) != 23) return "fail2"
    if (test3() != "OK") return "fail3"
    if (test4() != "OK") return "fail4"
    return "OK"
}