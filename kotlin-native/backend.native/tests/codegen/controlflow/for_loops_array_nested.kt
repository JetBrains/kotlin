package codegen.controlflow.for_loops_array_nested

import kotlin.test.*

@Test fun arrayOfArrays() {
    val metaArray = arrayOf(
            arrayOf(1, 2, 3),
            arrayOf("Hello"),
            arrayOf<Any>(),
            arrayOf(1..10)
    )
    for (array in metaArray) {
        inner@for (elem in array) {
            if (elem is IntProgression) {
                for (i in elem) {
                    print(i)
                }
                continue@inner
            } else {
                print(elem)
            }
        }
        println()
    }
}