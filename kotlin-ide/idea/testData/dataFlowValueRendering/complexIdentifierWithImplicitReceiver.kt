fun C.outer(a: Any?) {
    if (x is String) {
        <caret>null
    }
}

class C {
    val x: Any? = null
}