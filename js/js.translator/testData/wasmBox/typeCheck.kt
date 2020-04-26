
interface I1
interface I2
interface I3 : I1, I2

class A
class B : I1
class C : I3

fun box(): String {
    val a: Any? = A()
    if (a !is A) return "Fail 1"
    if (a is B) return "Fail 2"
    if (a !is Any) return "Fail 3"
    if (a !is Any?) return "Fail 4"
    if (a is B?) return "Fail 5"
    if (a !is A?) return "Fail 6"

    val b: Any? = B()
    if (a is I1) return "Fail 7"
    if (b !is I1) return "Fail 8"
    if (b is I2) return "Fail 9"
    if (b is I3) return "Fail 9.1"

    val c: Any? = C()
    if (c !is I1) return "Fail 10"
    if (c !is I2) return "Fail 11"
    if (c !is I3) return "Fail 12"

    return "OK"
}