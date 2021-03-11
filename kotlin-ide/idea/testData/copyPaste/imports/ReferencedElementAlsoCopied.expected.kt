// ERROR: Type mismatch: inferred type is Unit but A was expected
// ERROR: Function 'ext' must have a body
package to

interface T

class A(i: Int) {}

val c = 0

fun g(a: A) {}

fun A.ext()

object O1 {
    fun f() {
    }
}

object O2 {
}

class ClassObject {
    companion object {
    }
}

fun f(a: A, t: T) {
    g(A(c).ext())
    O1.f()
    O2
    ClassObject
}