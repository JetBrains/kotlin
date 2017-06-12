// EXPECTED_REACHABLE_NODES: 887
package foo


fun box(): String {
    val data = myArrayList("foo", "bar")
    if (data.myHead != "foo") {
        return "fail: ${data.myHead}"
    }
    return "OK"
}


inline public fun <T> myArrayList(vararg values: T): ArrayList<T> {
    val c = ArrayList<T>()
    for (v in values) {
        c.add(v)
    }
    return c
}

public val <T> ArrayList<T>.myHead: T
    get() {
        return get(0)
    }
