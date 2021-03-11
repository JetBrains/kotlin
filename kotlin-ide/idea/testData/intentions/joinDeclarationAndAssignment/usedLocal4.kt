class Foo(size: Int)

class Test(height: Int, width: Int) {
    private val size: Int = height * width
    private val <caret>data: Foo

    init {
        data = Foo(size)
    }
}