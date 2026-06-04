open class Base
class Derived : Base()
fun check(x: Base): Boolean = x !is Derived
fun main() {
    val d = Derived()
    val b = Base()
    if (check(d))  error("check(Derived()) = true, expected false")
    if (!check(b)) error("check(Base()) = false, expected true")
}
