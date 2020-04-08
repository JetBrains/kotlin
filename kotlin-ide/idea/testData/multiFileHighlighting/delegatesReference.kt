package test

import delegates.*
import util.*

fun f() {
    val d = D(C("", 0), object : T2 {})
    d.anotherFun()
    d.<error>invalidFun</error>()
    WithDelegatedProperty().i
}