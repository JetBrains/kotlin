package some

import enums.E
import enums.Base

fun f(<warning>e</warning>: Base) {

}

fun test() {
    f(E.E1)
    f(E.E2)
    f(E.E3)
    f(E.<error>E4</error>)
    f(E.<error>Invalid</error>)
    E.E1.f()
}