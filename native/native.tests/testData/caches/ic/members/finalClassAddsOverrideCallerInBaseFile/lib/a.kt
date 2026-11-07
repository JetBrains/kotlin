package test

open class A {
    open fun foo(): Int = 1
}

// The receiver B is final and has no own override of foo, so this call is devirtualized into a
// direct call of A.foo. The generated code then references no symbol of sub.kt, the file that
// declares B and decides what the devirtualized call may target.
fun useExplicit(b: B): Int = b.foo()
