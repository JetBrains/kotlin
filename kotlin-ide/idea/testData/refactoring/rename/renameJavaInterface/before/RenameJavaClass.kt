package kotlin.testing

import testing.SomeInterface

class Some(s: SomeInterface) : SomeInterface() {
    val test = s

    fun testFun(param : SomeInterface) : SomeInterface {
        return test;
    }
}