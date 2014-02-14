package foo

import java.util.*

fun box(): Boolean {
    val data = arrayList("foo", "bar")
    if (data.head != "foo") {
        return false
    }
    return true
}


public inline fun arrayList<T>(vararg values: T): ArrayList<T> {
    val c = ArrayList<T>()
    for (v in values) {
        c.add(v)
    }
    return c
}

public inline val <T> ArrayList<T>.head: T
    get() {
        return get(0)
    }