package test2

import test1.Changed

class Derived : Changed() {
    fun bar(): String = foo()
}
