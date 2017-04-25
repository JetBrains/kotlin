// EXPECTED_REACHABLE_NODES: 606
package foo

class MyList<T>(vararg val data: T) : AbstractList<T>() {
    override fun get(index: Int) = data[index]
    override val size: Int get() = data.size
}

fun <T> test(expected: String, list: List<T>) {
    var s = ""
    for (e in list) {
        s += "$e,"
    }
    assertEquals(expected, s)

    val it = list.iterator()
    s = ""
    while (it.hasNext()) {
        val e = it.next()
        s += "$e,"
    }
    assertEquals(expected, s)
}

fun box(): String {

    test("32,12,23,444,", MyList(32, 12, 23, 444))
    test("", MyList<Any>())

    return "OK"
}