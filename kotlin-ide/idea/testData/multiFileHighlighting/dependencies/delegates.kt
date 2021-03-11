package delegates

import util.*

class D(val t: T, val t2: T2): T by t, T2 by t2 {
    fun anotherFun() {
    }
}

class WithDelegatedProperty() {
    val i: Int by someInvalidCode
}