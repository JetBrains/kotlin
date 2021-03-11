// "Surround with lambda" "true"
fun subclass() {
    base(<caret>Leaf())
}

fun base(base: () -> Base) {}

open class Base {}
class Leaf : Base()