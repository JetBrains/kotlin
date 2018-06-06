class Foo {
    class Bar(val a: Int, val b: Int) {
        fun getAPlusB() = a + b
        class Baz {
            fun doNothing() = Unit
        }
    }
}