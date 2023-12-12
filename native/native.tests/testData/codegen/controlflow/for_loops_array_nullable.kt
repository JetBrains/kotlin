import kotlin.test.*

val sb = StringBuilder()

private fun nullableArray(a: Array<Int>): Array<Int>? {
    return a
}

fun box(): String {
    val array = arrayOf(1, 2, 3)
    nullableArray(array)?.let {
        for (elem in it) {
            sb.append(elem)
        }
    }

    assertEquals("123", sb.toString())
    return "OK"
}