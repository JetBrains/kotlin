fun main(klass: A) {
    <caret>if (a !is B) return
    val a = 1
}

open class A
open class B: A {
    fun funInB() {}
}

// INVOCATION_COUNT: 1
// RUNTIME_TYPE: B