package foo

import java.util.*

class A<T>(val list: MutableList<T>) {
    fun addAll(c: Collection<T>) {
        list.addAll(c)
    }
}

fun <T> A<T>.plusAssign(other: Collection<T>) {
    addAll(other)
}

fun box(): Boolean {
    var v1 = arrayListOf("foo")
    val v2 = listOf("bar")

    val a = A(v1)
    a += v2

    return (v1.size() == 2 && v1[0] == "foo" && v1[1] == "bar")
}
