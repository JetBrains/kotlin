package dataClasses

data class User(val firstName: String, val lastName: String, val age: Int) {
    enum class Foo {
        FOO, BAR
    }

    interface Intf {
        fun foo() {}
    }
}

data class Either<F, S>(val first: F, val second: S)