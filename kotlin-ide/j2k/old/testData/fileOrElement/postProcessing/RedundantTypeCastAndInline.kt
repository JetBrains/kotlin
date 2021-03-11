internal class C {
    fun foo(o: Any) {
        if (o is String) {
            val l = o.length
            val substring = o.substring(l - 2)
        }
    }
}