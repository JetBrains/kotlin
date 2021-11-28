package a

class E(val x: String) {
    inner class Inner {
        inline fun foo(y: String) = x + y
    }
}