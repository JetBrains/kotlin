// IS_APPLICABLE: true
fun foo() {
    val x = <caret>Box(Box("x"))
}

class Box<T>(t : T) {
    var value = t
}