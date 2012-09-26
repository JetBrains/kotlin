package regressions

import kotlin.test.assertEquals
import org.junit.Test as test

fun f(xs: Iterator<Int>): Int {
    var answer = 0
    for (x in xs)  {
        answer += x
    }
    return answer
}

class Kt2495Test {
    test fun duplicateIteratorsBug() {
        val list = arrayList(1, 2, 3)
        val result = f(list.iterator())
        assertEquals(6, result)
    }
}