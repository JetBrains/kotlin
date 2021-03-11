// IS_APPLICABLE: false
fun foo() {
    val x = Box<caret><Any>("x")
}

class Box<T>(t : T) {
    var value = t
}