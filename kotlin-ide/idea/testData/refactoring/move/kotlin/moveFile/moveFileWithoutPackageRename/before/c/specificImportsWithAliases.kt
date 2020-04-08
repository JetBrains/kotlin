package c

import a.Test as _Test
import a.test as _test
import a.TEST as _TEST

fun bar() {
    val t: _Test = _Test()
    _test()
    println(_TEST)
    _TEST = ""
}
