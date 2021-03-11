fun foo(a: Int, b: Int, c: Int, d: Int) : Boolean {
    return !(a < b && c ==<caret> d && a < d)
}