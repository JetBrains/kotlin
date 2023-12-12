package codegen.controlflow.for_loops_array_side_effects

import kotlin.test.*

private fun <T> sideEffect(array: T): T {
    println("side-effect")
    return array
}

@Test fun runTest() {
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
}