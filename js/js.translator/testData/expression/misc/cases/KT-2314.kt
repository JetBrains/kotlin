package foo

import java.util.*

fun box(): Boolean {
    val data = myArrayList("foo", "bar")
    if (data.myHead != "foo") {
        return false
    }
    return true
}


inline public fun myArrayList<T>(vararg values: T): ArrayList<T> {
    val c = ArrayList<T>()
    for (v in values) {
        c.add(v)
    }
    return c
}

inline public val <T> ArrayList<T>.myHead: T
    get() {
        return get(0)
    }
