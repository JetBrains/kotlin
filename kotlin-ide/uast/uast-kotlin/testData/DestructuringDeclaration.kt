fun foo(data: Any) {
    val (a, b) = "foo" to 1

    @Suppress("UNCHECKED_CAST")
    val (k, v) = data as Pair<String, String>
}
