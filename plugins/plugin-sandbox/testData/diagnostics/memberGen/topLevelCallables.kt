package foo

import org.jetbrains.kotlin.fir.plugin.DummyFunction

@DummyFunction
class MySuperClass {
    fun test() {
        val s = dummyMySuperClass(this)
        takeString(s)
    }
}

fun takeString(s: String) {}
