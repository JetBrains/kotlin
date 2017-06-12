// EXPECTED_REACHABLE_NODES: 888
package foo


fun <T> ArrayList<T>.findAll(predicate: (T) -> Boolean): ArrayList<T> {
    val result = ArrayList<T>()
    for (t in this) {
        if (predicate(t)) result.add(t)
    }
    return result
}


fun box(): String {
    val list: ArrayList<Int> = ArrayList<Int>()

    list.add(2)
    list.add(3)
    list.add(5)


    val m: ArrayList<Int> = list.findAll<Int>({ name: Int -> name < 4 })
    return if (m.size == 2) "OK" else "fail"
}
