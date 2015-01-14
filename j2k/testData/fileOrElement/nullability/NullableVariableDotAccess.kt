fun foo(s: String?, b: Boolean): Int {
    if (s == null) System.out.println("null")
    if (b) return s!!.length()
    return 10
}