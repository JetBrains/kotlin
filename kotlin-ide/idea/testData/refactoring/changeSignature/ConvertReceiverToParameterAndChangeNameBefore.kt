class X(val k: Int)

fun X.<caret>foo(n: Int): Boolean {
    return this.k > n
}