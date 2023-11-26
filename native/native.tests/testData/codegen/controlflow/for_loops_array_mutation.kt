// OUTPUT_DATA_FILE: for_loops_array_mutation.out


import kotlin.test.*

fun box(): String {
    val intArray = arrayOf(4, 0, 3, 5)

    for (element in intArray) {
        intArray[2] = 0
        intArray[3] = 0
        print(element)
    }
    return "OK"
}
