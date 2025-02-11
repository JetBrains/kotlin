// LL_FIR_DIVERGENCE
// KT-75132
// LL_FIR_DIVERGENCE
package foo

import org.jetbrains.kotlin.plugin.sandbox.DummyFunction

@DummyFunction
class MySuperClass {
    fun test() {
        val s = <!UNRESOLVED_REFERENCE!>dummyMySuperClass<!>(this)
        takeString(s)
    }
}

fun takeString(s: String) {}
