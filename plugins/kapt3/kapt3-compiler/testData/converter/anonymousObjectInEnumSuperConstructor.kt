interface Callback {
    fun invoke(): String
}

enum class Foo(val callback: Callback) {
    FOO(
        object : Callback {
            override fun invoke(): String = "OK"
        }
    )
}
