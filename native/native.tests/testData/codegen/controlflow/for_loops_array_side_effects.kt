import kotlin.test.*

val sb = StringBuilder()

private fun <T> sideEffect(array: T): T {
    sb.appendLine("side-effect")
    return array
}

fun box(): String {
    val intArray = intArrayOf(4, 0, 3, 5)

    val emptyArray = arrayOf<Any>()

    for (element in sideEffect(intArray)) {
        sb.append(element.toString())
    }
    sb.appendLine()
    for (element in sideEffect(emptyArray)) {
        sb.append(element.toString())
    }
    sb.appendLine()

    assertEquals("""
        side-effect
        4035
        side-effect


        """.trimIndent(), sb.toString())

    return "OK"
}