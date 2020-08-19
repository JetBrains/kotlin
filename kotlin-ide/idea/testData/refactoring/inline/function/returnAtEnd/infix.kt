fun main() {
    "Bob".oldFunc<caret>tion("Doug")
}

infix fun Any.newFunction(that: Any) = print("$that and $this")

fun Any.oldFunction(that: Any) {
    this newFunction that
}