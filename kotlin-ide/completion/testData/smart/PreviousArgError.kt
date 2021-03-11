class A
class B
class C
class D
class E

fun foo(s: String, a: A){}
fun foo(p: Int, b: B, c: C){}
fun foo(s: String, d: D, e: E, a: A){}

fun f(pa: A, pb: B, pc: C, pd: D, pe: E) {
    foo(xxx, pb, <caret>)
}

// EXIST: pc
// ABSENT: pa, pb, pd, pe
