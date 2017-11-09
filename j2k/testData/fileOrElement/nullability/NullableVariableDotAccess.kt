fun foo(s: String?, b: Boolean): Int {
    if (s == null) println("null")
    return if (b) s!!.length else 10
}