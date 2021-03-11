package a

import a.E.ENTRY
import a.Outer.*

interface T

class A(i: Int) {}

val c = 0

fun g(a: A) {}

fun A.ext() = A()

object O1 {
    fun f() {
    }
}

object O2 {
}

enum class E {
    ENTRY
}

class Outer {
    inner class Inner {
    }
    class Nested {
    }
    enum class NestedEnum {
    }
    object NestedObj {
    }
    interface NestedTrait {
    }
    annotation class NestedAnnotation
}

class ClassObject {
    companion object {
    }
}

<selection>private fun f(p: A, t: T) {
    g(A(c).ext())
    O1.f()
    O2
    ENTRY
}

private fun f2(i: Inner, n: Nested, e: NestedEnum, o: NestedObj, t: NestedTrait, a: NestedAnnotation) {
    ClassObject
}</selection>