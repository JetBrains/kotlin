// EXPECTED_REACHABLE_NODES: 892

class ArrayWrapper<T>() {
    val contents = ArrayList<T>()

    fun add(item: T) {
        contents.add(item)
    }

    operator fun plus(rhs: ArrayWrapper<T>): ArrayWrapper<T> {
        val result = ArrayWrapper<T>()
        result.contents.addAll(contents)
        result.contents.addAll(rhs.contents)
        return result
    }

    fun get(index: Int): T {
        return contents.get(index)
    }
}

fun box(): String {
    var v1 = ArrayWrapper<String>()
    val v2 = ArrayWrapper<String>()
    v1.add("foo")
    val v3 = v1
    v2.add("bar")
    v1 += v2
    return if (v1.contents.size == 2 && v3.contents.size == 1) "OK" else "fail"
}
