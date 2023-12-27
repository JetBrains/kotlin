package test

class Class {
    val String.first
        get() = this
    @JvmField
    val second = Unit
    @JvmField
    val first = Unit
}
