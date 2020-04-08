package kotlin.testing

import testing.NewInterface

class New(s: NewInterface) : NewInterface() {
    val test = s

    fun testFun(param : NewInterface) : NewInterface {
        return test;
    }
}