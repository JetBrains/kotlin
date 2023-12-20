// MODULE: lib
// FILE: lib.kt

package a

import kotlin.reflect.KProperty

public val sb = StringBuilder()

open class A {
    open val x = 42
}

class Delegate {
    val f = 117
    operator fun getValue(receiver: Any?, p: KProperty<*>): Int {
        sb.appendLine(p.name)
        return f
    }
}

open class B: A() {
    override val x: Int by Delegate()

    fun bar() {
        sb.appendLine(super<A>.x)
    }
}

// MODULE: main(lib)
// FILE: main.kt

import a.*
import kotlin.test.*

open class C: B() {
    override val x: Int = 156

    fun foo() {
        sb.appendLine(x)

        sb.appendLine(super<B>.x)
        bar()
    }
}

fun box(): String {
    val c = C()
    c.foo()

    assertEquals("""
    156
    x
    117
    42

    """.trimIndent(), sb.toString())

    return "OK"
}
