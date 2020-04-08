// "Remove 'val' from parameter" "true"
open class Base(open <caret>val x: Int) {
    val y = x
}

class Derived(y: Int) : Base(y)