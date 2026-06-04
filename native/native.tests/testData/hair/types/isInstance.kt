open class Base
class Derived : Base()
fun check(x: Base): Boolean = x is Derived
fun main() {
    val d = Derived()
    val b = Base()
    if (!check(d)) error("check(Derived()) = false, expected true")
    if (check(b))  error("check(Base()) = true, expected false")
}
