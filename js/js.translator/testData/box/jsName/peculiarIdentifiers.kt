// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1294
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
    val `1(¢)` = "OK"
    fun `[£]`() = `1(¢)`
    return `[£]`()
}

inline fun <reified `-`> test5(): String = `-`::class.simpleName!!

inline fun <reified `-`> test6(x: Any): Boolean = x is `-`

class OK

fun box(): String {
    if (test1() != 20) return "fail1"
    if (test2(10, 13) != 23) return "fail2"
    if (test3() != "OK") return "fail3"
    if (test4() != "OK") return "fail4"

    if (test5<OK>() != "OK") return "fail5"
    if (!test6<String>("foo")) return "fail6"

    return "OK"
}