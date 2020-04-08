// WITH_LIBRARY: copyPaste/imports/KotlinLibrary

package a

import d.*
import d.E.ENTRY
import d.Outer.*

<selection>fun f(a: A, t: T) {
    g(A(c).ext())
    O1.f()
    O2
    ENTRY
}

fun f2(i: Inner, n: Nested, e: NestedEnum, o: NestedObj, t: NestedTrait, a: NestedAnnotation) {
    ClassObject
}</selection>