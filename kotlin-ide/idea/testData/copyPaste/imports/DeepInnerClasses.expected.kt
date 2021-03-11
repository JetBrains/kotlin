package to

import a.Outer
import a.with

fun f(p1: Outer.Nested.NN, p2: Outer.Nested.NI, p3: Outer.Inner.II) {
    Outer.Nested.NN2()
    with(Outer.Nested()) {
        NI2()
    }
    with(Outer().Inner()) {
        II2()
    }
}