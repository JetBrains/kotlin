// COMPILER_PLUGIN: power-assert-compiler-plugin.jar function=kotlin.assert
// ASSERTIONS_MODE: always-enable

fun a(): Int = 1
fun b(): Int = 2

fun fail() {
    assert(a() == b())
}

fun box(): String {
    val message = try {
        fail()
        "Function did not fail"
    } catch (e: Throwable) {
        e.message.orEmpty()
    }

    val expectedMessage = """

assert(a() == b())
       |   |  |
       |   |  2
       |   false
       1

    """.trimIndent()

    return if (message == expectedMessage) "OK"
    else message
}
