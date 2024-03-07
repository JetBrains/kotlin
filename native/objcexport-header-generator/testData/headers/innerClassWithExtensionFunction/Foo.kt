class Foo {
    inner class Inner {
        val Foo.propVal: Boolean
            get() = false
        fun String.foo() = 42
        var Foo.propSet: Int
            get() = 42
            set(value) {}
    }
}