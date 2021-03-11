fun foo(a: Boolean, c: Int, d: Int) : Boolean {
    return !(a <caret>&& c == d)
}