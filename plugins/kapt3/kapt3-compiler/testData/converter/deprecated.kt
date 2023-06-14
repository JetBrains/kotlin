package deprecated

@Deprecated("Deprecated annotation")
annotation class Anno

@Deprecated("Deprecated class")
@Anno
class Foo {
    @Deprecated("Deprecated function")
    fun foo(a: Int) {}

    @Deprecated("Deprecated property")
    val prop = 0

    var foo: Int
        @Deprecated("Deprecated getter") get() = 0
        @Deprecated("Deprecated setter") set(value) {}
}
