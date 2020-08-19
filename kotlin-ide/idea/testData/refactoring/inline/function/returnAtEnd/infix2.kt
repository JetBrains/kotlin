fun main() {
    "Bob".oldFunction("Doug")
}

infix fun Any.newFunction(that: Any) = print("$that and $this")

fun Any.oldFunction(that: Any) = this new<caret>Function that