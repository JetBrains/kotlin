package foo

import java.util.ArrayList;

// TODO: drop when listOf will be available here.
fun listOf<T>(vararg a: T): List<T> {
    val list = ArrayList<T>();

    for (e in a) {
        list.add(e)
    }

    return list
}

fun test<T>(list: List<T>, elements: List<T>, expected: List<Int>, method: List<T>.(T) -> Int, methodName: String): String? {
    for (i in 0..elements.size() - 1) {
        val actual = list.method(elements[i])
        if (actual != expected[i]) return "$methodName failed when find: ${elements[i]}, expected: ${expected[i]}, actual: $actual"
    }
    return null
}

fun box(): String {
    //                0   1    2   3    4   5    6   7     8    9
    val list = listOf(3, "2", -1, null, 0, "2", -1, null, 0, 8)
    val findingElements = listOf(77, null, 0, 8, -15, "2", 5, 3, -1, "3")
    val expectedIndexOf = listOf(-1, 3, 4, 9, -1, 1, -1, 0, 2, -1)
    val expectedLastIndexOf = listOf(-1, 7, 8, 9, -1, 5, -1, 0, 6, -1)

    return test(list, findingElements, expectedIndexOf, { indexOf(it) }, "indexOf") ?:
    test(list, findingElements, expectedLastIndexOf, { lastIndexOf(it) }, "lastIndexOf") ?:
    "OK"
}