package foo

fun box(): Boolean {
    return (!(A().myInlineMethod()))
}

class A() {
    inline fun myInlineMethod(): Boolean {
        return false
    }
}