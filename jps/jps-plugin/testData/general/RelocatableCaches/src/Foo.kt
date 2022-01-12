open class Foo() {
    companion object {
        const val CONST = 0
    }

    inline fun bar() = 1
}

class FooChild() : Foo() {}