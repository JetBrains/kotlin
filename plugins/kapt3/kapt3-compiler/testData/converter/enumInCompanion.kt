class Test {
    private val foo = Example.FOO

    companion object {
        enum class Example { FOO }
    }
}

class Test2 {
    private val foo = Example.FOO

    companion object Amigo {
        enum class Example { FOO }
    }
}

class Test3 {
    private val foo = Amigo.Example.FOO

    object Amigo {
        enum class Example { FOO }
    }
}

class Test4 {
    private val foo = Foo.constProperty

    companion object {
        object Foo {
            const val constProperty = 1
        }
    }
}

class Test5 {
    private val foo = Amigos.Companion.Goo.Example.FOO

    class Amigos {
        companion object {
            class Goo {
                enum class Example { FOO }
            }
        }
    }
}