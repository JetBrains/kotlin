class A {
    internal fun foo(array: Array<String?>) {
        for (i in array.size downTo 0) {
            println(i)
        }
    }
}
