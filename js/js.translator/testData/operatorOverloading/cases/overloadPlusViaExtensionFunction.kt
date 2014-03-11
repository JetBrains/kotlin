package foo

import java.util.*

fun <T> ArrayList<T>.plus(other: Collection<T>): List<T> {
    val c = ArrayList<T>()
    c.addAll(this)
    c.addAll(other)
    return c
}

fun box(): Boolean {
    var v1 = ArrayList<String>()
    v1.add("foo")
    val v2 = ArrayList<String>()
    v2.add("bar")
    val v = v1 + v2

    return (v.size() == 2 && v[0] == "foo" && v[1] == "bar")
}
