// IS_APPLICABLE: true
fun foo() {
    val x = Box<caret><Box<String>>(Box("x"))
}

class Box<T>(t : T) {
    var value = t
}