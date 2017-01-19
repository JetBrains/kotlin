internal class C {
    fun foo(o: Any) {
        if (o is String) {
            val s = o
            val l = s.length
            val substring = s.substring(l - 2)
        }
    }
}