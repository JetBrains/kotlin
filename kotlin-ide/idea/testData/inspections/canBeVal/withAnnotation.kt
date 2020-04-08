fun foo(p: List<Any>) {
    @Suppress("UNCHECKED_CAST")
    var v = p as List<String>
    print(v)
}