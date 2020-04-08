abstract class Base(val x: Int)

class Derived<caret>(x: Int) : Base(x) {
    val y = 2 * x
}