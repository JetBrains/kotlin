package p6

import p5.O1.xxx_fun2FromP5O1
import p4.O1
import p3.*
import p2.O1

fun foo() {
    xxx_<caret>
}

// INVOCATION_COUNT: 2
// ORDER: xxx_fun2FromP5O1
// ORDER: xxx_fun1FromP5O1
// ORDER: xxx_fun1FromP6O1
// ORDER: xxx_fun1FromP4O1
// ORDER: xxx_fun1FromP3O1
// ORDER: xxx_fun1FromP2O2
// ORDER: xxx_fun1FromP1O1
