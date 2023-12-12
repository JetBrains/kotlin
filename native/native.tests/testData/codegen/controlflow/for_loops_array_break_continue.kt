import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    val intArray = intArrayOf(4, 0, 3, 5)

    val emptyArray = arrayOf<Any>()

    for (element in intArray) {
        sb.append(element)
        if (element == 3) {
            break
        }
    }
    sb.appendLine()
    for (element in emptyArray) {
        sb.append(element)
    }
    sb.appendLine()

    assertEquals("403\n\n", sb.toString())
    return "OK"
}