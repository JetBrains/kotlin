typealias MyThrows = kotlin.Throws

class Foo {
    @kotlin.Throws(Exception::class)
    fun noalias() {}

    @MyThrows(Exception::class)
    fun aliased() {}
}
