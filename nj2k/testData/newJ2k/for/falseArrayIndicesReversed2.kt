class A {
    internal fun foo(array: Array<String?>) {
        for (i in array.size - 2 downTo 0) {
            println(i)
        }
    }
}
