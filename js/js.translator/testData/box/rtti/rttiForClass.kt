// EXPECTED_REACHABLE_NODES: 503
package foo

class D

open class A

open class B : A()

open class C : B()

fun box(): String {
    val a: Any = A()
    val b: Any = B()
    val c: Any = C()
    if (a !is A) return "a !is A"

    val t = a is A
    if (!t) return "t = a is A; t != true"

    if (b !is A) return "b !is A"
    if (b !is B) return "b !is B"

    if (c !is A) return "c !is A"
    if (c !is B) return "c !is B"
    if (c !is C) return "c !is C"

    if (a is D) return "a is D"
    if (b is D) return "b is D"

    return "OK"
}