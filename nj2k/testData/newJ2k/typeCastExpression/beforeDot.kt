class A {
    fun foo(o: Any?) {
        if (o == null) return
        val length = (o as String).length
    }
}