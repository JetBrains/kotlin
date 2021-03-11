// NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS: 2
package test

import dependency.O.foo
import dependency.O.bar
import dependency.E1.A1
import dependency.E1.B1
import dependency.E2.A2
import dependency.E3.*

fun f() {
    foo()
    bar()
    val v1 = A1
    val v2 = B1
    val v3 = A2
    val v4 = A3
}