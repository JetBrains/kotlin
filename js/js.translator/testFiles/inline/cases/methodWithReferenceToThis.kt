package foo

fun box(): Boolean {
    return !(A(false).myInlineMethod()) and A(true).myInlineMethod()
}

class A(val a: Boolean) {
    inline fun myInlineMethod(): Boolean {
        return a
    }
}