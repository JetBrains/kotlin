class Foo {
    fun someMethodWithDiffReturnType(): String = ""
    fun someMethodWithSameReturnType() = Unit
}

class Bar {
    fun someMethodWithDiffReturnType(): Int = 0
    fun someMethodWithSameReturnType() = Unit
}