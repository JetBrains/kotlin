package test

@JvmField
internal val a: String = ""

internal val b: String = ""

fun emptyKtClassWorkaround() = Unit
class Class {
    @JvmField
    internal val a: String = ""

    internal val b: String = ""
}
