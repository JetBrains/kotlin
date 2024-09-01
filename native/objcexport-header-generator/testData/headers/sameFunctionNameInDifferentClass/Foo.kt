class Foo {
    fun someMethodWithDiffReturnType(): String = ""
    fun someMethodWithSameReturnType() = Unit
}

class Bar {
    fun someMethodWithDiffReturnType(): Int
    fun someMethodWithSameReturnType() = Unit
}