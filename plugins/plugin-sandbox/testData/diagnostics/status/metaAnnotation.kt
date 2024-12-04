// FILE: first.kt

@Open
class A {
    fun foo() {

    }
}

@Open
class B : A() {
    <!OVERRIDING_FINAL_MEMBER!>override<!> fun foo() {

    }
}

// FILE: second.kt
import org.jetbrains.kotlin.plugin.sandbox.AllOpen

@AllOpen
annotation class Open
