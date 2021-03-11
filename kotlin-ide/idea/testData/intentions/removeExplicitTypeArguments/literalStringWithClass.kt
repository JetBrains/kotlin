// IS_APPLICABLE: true
fun foo() {
    val x = Box<caret><String>("x")
}

class Box<T>(t : T) {
    var value = t
}