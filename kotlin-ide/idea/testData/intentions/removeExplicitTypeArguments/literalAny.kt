// IS_APPLICABLE: true
fun foo() {
    val x = Box<caret><Any>(Any())
}

class Box<T>(t : T) {
    var value = t
}