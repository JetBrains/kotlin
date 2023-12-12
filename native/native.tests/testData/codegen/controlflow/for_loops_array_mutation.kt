import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    val intArray = arrayOf(4, 0, 3, 5)

    for (element in intArray) {
        intArray[2] = 0
        intArray[3] = 0
        sb.append(element)
    }

    assertEquals("4000", sb.toString())
    return "OK"
}