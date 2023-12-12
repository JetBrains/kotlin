package codegen.controlflow.for_loops_array

import kotlin.test.*

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

@Test fun runTest() {
    val intArray = intArrayOf(4, 0, 3, 5)

    val emptyArray = arrayOf<Any>()

    for (element in intArray) {
        print(element)
    }
    println()
    for (element in emptyArray) {
        print(element)
    }
    println()

    val byteArray = byteArrayOf(1, -1)
    println(genericArray(byteArray))

    val fives = intArrayOf(5, 5, 5, -5, -5, -5)
    println(fives.sum())
}