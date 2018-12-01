internal class C {
    fun foo(o: Any?) {
        if (o !is String) return
        println("String")
    }
}
