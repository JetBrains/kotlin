package codegen.controlflow.for_loops_array_nullable

import kotlin.test.*

private fun nullableArray(a: Array<Int>): Array<Int>? {
    return a
}

@Test fun nullable() {
    val array = arrayOf(1, 2, 3)
    nullableArray(array)?.let {
        for (elem in it) {
            print(elem)
        }
    }
}