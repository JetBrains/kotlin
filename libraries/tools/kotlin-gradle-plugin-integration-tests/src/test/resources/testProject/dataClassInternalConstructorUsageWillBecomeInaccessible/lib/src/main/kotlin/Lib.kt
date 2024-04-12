@ExposedCopyVisibility
data class Foo private constructor(val x: Int) {
    companion object {
        fun new() = Foo(1)
    }
}
