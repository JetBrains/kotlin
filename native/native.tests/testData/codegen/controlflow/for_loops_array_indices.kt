import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    val intArray = intArrayOf(4, 0, 3, 5)

    val emptyArray = arrayOf<Any>()

    for (index in intArray.indices) {
        sb.append(index)
    }
    sb.appendLine()
    for (index in emptyArray.indices) {
        sb.append(index)
    }
    sb.appendLine()

    assertEquals("0123\n\n", sb.toString())
    return "OK"
}