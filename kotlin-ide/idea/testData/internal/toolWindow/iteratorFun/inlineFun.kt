package inlineFun1

class A() {
    inline fun iterator() : Iterator<Int> = throw UnsupportedOperationException()
}