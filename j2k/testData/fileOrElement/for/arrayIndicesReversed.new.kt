class A {
    internal fun foo(array: Array<String?>) {
        for (i in array.indices.reversed()) {
            println(i)
        }
    }
}
