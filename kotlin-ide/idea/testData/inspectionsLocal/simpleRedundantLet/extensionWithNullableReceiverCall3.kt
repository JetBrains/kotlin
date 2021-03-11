// WITH_RUNTIME
// PROBLEM: none
class A(val b: B?)
class B
class C(val d: D)
class D

fun B?.getC(): C {
    return C(D())
}

fun test(a: A?) {
    a?.let<caret> { it.b.getC().d }
}