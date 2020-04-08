class C {
    val a: Any? = null

    fun test() {
        if (a is String) {
            <caret>null
        }
    }
}