// OUTPUT_DATA_FILE: for_loops_array_indices.out


import kotlin.test.*

fun box(): String {
    val intArray = intArrayOf(4, 0, 3, 5)

    val emptyArray = arrayOf<Any>()

    for (index in intArray.indices) {
        print(index)
    }
    println()
    for (index in emptyArray.indices) {
        print(index)
    }
    println()

    return "OK"
}
