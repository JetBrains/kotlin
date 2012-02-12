package test.regressions.kt1172

import std.concurrent.*
import junit.framework.*
import java.util.*

public fun scheduleRefresh(vararg files : Object) {
    java.util.ArrayList<Object>(files.map{ it })
}

fun main(args : Array<String?>?) {
}

class Kt1172Test() : TestCase() {
    fun testMe() {
        main(null)
    }
}
