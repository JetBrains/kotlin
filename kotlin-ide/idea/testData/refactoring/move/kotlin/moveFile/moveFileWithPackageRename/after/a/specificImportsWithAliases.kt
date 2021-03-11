package a

import b.Test as _Test
import b.test as _test
import b.TEST as _TEST

fun bar() {
    val t: _Test = _Test()
    _test()
    t._test()
    println(_TEST)
    println(t._TEST)
    _TEST = ""
    t._TEST = ""
}
