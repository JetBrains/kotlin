package codegen.controlflow.for_loops_array_break_continue

import kotlin.test.*

@Test fun runTest() {
    val intArray = intArrayOf(4, 0, 3, 5)

    val emptyArray = arrayOf<Any>()

    for (element in intArray) {
        print(element)
        if (element == 3) {
            break
        }
    }
    println()
    for (element in emptyArray) {
        print(element)
    }
    println()
}