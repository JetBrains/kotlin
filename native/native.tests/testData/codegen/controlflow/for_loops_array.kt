import kotlin.test.*

val sb = StringBuilder()

fun <T : ByteArray> genericArray(data : T): Int {
    var sum = 0
    for (element in data) {
        sum += element
    }
    return sum
}

fun IntArray.sum(): Int {
    var sum = 0
    for (element in this) {
        sum += element
    }
    return sum
}

fun box(): String {
    val intArray = intArrayOf(4, 0, 3, 5)

    val emptyArray = arrayOf<Any>()

    for (element in intArray) {
        sb.append(element)
    }
    sb.appendLine()
    for (element in emptyArray) {
        sb.append(element)
    }
    sb.appendLine()

    val byteArray = byteArrayOf(1, -1)
    sb.append(genericArray(byteArray))
    sb.appendLine()

    val fives = intArrayOf(5, 5, 5, -5, -5, -5)
    sb.append(fives.sum())
    sb.appendLine()

    assertEquals("4035\n\n0\n0\n", sb.toString())
    return "OK"
}