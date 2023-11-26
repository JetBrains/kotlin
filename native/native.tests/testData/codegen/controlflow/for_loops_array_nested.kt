// OUTPUT_DATA_FILE: for_loops_array_nested.out


import kotlin.test.*

fun box(): String {
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
    return "OK"
}
