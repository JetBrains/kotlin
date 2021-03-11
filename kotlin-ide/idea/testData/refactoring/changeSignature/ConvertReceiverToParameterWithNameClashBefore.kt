val x1: Int

class X(val x: Int)

fun X.<caret>foo(x: Int): Boolean {
    return this.x + x > x1
}