// WITH_RUNTIME
// IS_APPLICABLE: false

class List {
    val size = 0
}

fun foo() {
    val list = List()
    list.size<caret> > 0
}