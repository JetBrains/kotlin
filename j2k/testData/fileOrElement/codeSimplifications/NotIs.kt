class C {
    fun foo(o: Any) {
        if (o !is String) return
        System.out.println("String")
    }
}
