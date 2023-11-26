// OUTPUT_DATA_FILE: for_loops_array_side_effects.out


import kotlin.test.*

private fun <T> sideEffect(array: T): T {
    println("side-effect")
    return array
}

fun box(): String {
    val intArray = intArrayOf(4, 0, 3, 5)

    val emptyArray = arrayOf<Any>()

    for (element in sideEffect(intArray)) {
        print(element)
    }
    println()
    for (element in sideEffect(emptyArray)) {
        print(element)
    }
    println()

    return "OK"
}
