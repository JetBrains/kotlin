// CORRECT_ERROR_TYPES

context(t: U1)
var U2.s: U3 get() = ""

context(t: U1)
var U2.s1: String get() = ""

context(t: U1)
fun f(x: U2) {}

context(t: U1)
fun U2.f1(x: U3) {}
