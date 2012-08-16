package foo

import java.util.*
import java.lang.*
import jet.Iterator

public inline fun <T, C: Collection<in T>> Iterator<T>.takeWhileTo(result: C, predicate: (T) -> Boolean) : C {
    for (element in this) if (predicate(element)) result.add(element) else break
    return result
}

fun box(): Any? {
    val c = ArrayList<Int>()
    for (i in 0..5) {
        c.add(i)
    }

    val d = ArrayList<Int>()

    c.iterator().takeWhileTo(d, {i -> i < 4 })
    return d.size() == 4
}