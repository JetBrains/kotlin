package foo

fun box(): Boolean {
    return !(A(false).wrap()) and A(true).wrap()
}

class A(val a: Boolean) {
    inline fun myInlineMethod(): Boolean {
        return a
    }

    fun wrap() = myInlineMethod()
}