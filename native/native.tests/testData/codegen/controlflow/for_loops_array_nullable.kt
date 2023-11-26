// OUTPUT_DATA_FILE: for_loops_array_nullable.out


import kotlin.test.*

private fun nullableArray(a: Array<Int>): Array<Int>? {
    return a
}

fun box(): String {
    val array = arrayOf(1, 2, 3)
    nullableArray(array)?.let {
        for (elem in it) {
            print(elem)
        }
    }
    return "OK"
}
