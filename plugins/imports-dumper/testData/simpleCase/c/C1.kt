package c

import a.A1
import a.A2
import b.B1.Companion.a2
import b.nestedB.bar
import b.nestedB.foo

class C1 {
    val x = foo(TODO())
    val y = bar(TODO())

    val z: A1? = null
    val t: A2? = a2

}