package test

object Object {
    // Value is changed, but we don't care, it is not compile-time constant, and therefore can't be inlined
    val CONST = "old"
}
