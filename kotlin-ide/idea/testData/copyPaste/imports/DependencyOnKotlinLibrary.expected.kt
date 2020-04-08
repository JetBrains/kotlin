// ERROR: Type mismatch: inferred type is Unit but A was expected
package to

import d.A
import d.ClassObject
import d.E
import d.O1
import d.O2
import d.Outer
import d.T
import d.c
import d.ext
import d.g

fun f(a: A, t: T) {
    g(A(c).ext())
    O1.f()
    O2
    E.ENTRY
}

fun f2(i: Outer.Inner, n: Outer.Nested, e: Outer.NestedEnum, o: Outer.NestedObj, t: Outer.NestedTrait, a: Outer.NestedAnnotation) {
    ClassObject
}