package test


class Class {
    @JvmField
    val first = Unit
    @JvmField
    val second = Unit
    val String.first
        get() = this
}
