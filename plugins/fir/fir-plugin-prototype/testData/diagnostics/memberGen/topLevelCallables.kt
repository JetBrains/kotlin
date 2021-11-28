package foo

import org.jetbrains.kotlin.fir.plugin.A

@A
class MySuperClass {
    fun test() {
        val s = dummyMySuperClass(this)
        takeString(s)
    }
}

fun takeString(s: String) {}
