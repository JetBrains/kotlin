package c

import b.TEST
import b.Test
import b.test

fun bar() {
    val t: Test = Test()
    test()
    println(TEST)
    TEST = ""
}
