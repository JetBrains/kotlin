package foo

import java.util.*

fun <T> ArrayList<T>.plusAssign(other: Collection<T>) {
    addAll(other)
}

fun box(): Boolean {
    var v1 = ArrayList<String>()
    val v2 = ArrayList<String>()
    v1.add("foo")
    v2.add("bar")
    v1 += v2
    return (v1.size() == 2 && v1[0] == "foo" && v1[1] == "bar")
}
