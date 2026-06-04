open class Base
class Derived : Base()
fun downcast(x: Base): Derived = x as Derived
fun main() {
    val d = Derived()
    val r = downcast(d)
    if (r !== d) error("downcast returned different object")
}
