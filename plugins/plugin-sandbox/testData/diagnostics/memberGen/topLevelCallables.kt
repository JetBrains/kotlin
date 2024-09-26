package foo

import org.jetbrains.kotlin.plugin.sandbox.DummyFunction

@DummyFunction
class MySuperClass {
    fun test() {
        val s = dummyMySuperClass(this)
        takeString(s)
    }
}

fun takeString(s: String) {}
