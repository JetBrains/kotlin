package c

import a.Test
import a.test
import a.TEST

fun bar() {
    val t: Test = Test()
    test()
    t.test()
    println(TEST)
    println(t.TEST)
    TEST = ""
    t.TEST = ""
}
