fun outer(a: Any?) {
    fun inner(b: Any?) {
        if (a is String && b is String) {
            <caret>null
        }
    }
}