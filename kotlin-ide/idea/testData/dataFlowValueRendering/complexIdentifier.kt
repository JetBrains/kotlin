fun outer(c: C) {
    if (c.x is String) {
        <caret>null
    }
}

class C {
    val x: Any? = null
}