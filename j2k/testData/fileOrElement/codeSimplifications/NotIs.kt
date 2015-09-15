internal class C {
    internal fun foo(o: Any) {
        if (o !is String) return
        println("String")
    }
}
