package c

import a.Test
import a.test
import a.TEST

fun bar() {
    val t: Test = Test()
    test()
    println(TEST)
    TEST = ""
}
