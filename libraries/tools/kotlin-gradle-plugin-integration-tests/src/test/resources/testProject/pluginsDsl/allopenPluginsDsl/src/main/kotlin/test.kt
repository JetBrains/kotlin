package test

import lib.*

@AllOpen
class OpenClass {
    fun method() {}
}

@AllClose // effectively does nothing
class ClosedClass {
    fun method() {}
}