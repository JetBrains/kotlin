external interface Foo {
    val value: String
}

data class FooImpl(override val value: String): Foo