// SNIPPET

class Foo {
    companion object {
        fun hello(): String = "Hello"
    }

    class Bar {
        companion object {
            fun hello2(): String = "Hello2"
        }
    }
}

// SNIPPET

val res = Foo.hello()
val res2 = Foo.Bar.hello2()

// EXPECTED: res == Hello
// EXPECTED: res2 == Hello2
