package ppp

import dependency1.xxx
import dependency2.xxx

interface I

fun I.xxx() {}

class C : I {
    fun foo() {
        xx<caret>
    }
}

// EXIST: { lookupString: "xxx", itemText: "xxx", tailText: "() for C in dependency2", typeText: "Int" }
// NOTHING_ELSE
