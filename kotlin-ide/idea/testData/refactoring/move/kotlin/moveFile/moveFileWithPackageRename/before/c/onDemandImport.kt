package c

import a.*

fun bar() {
    val t: Test = Test()
    test()
    t.test()
    println(TEST)
    println(t.TEST)
    TEST = ""
    t.TEST = ""
}
