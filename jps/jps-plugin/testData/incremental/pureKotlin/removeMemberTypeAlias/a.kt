package a

class Outer {
    inner class B(x: String)

    @Suppress("TOPLEVEL_TYPEALIASES_ONLY", "WRONG_MODIFIER_TARGET")
    inner typealias A1 = B

    @Suppress("TOPLEVEL_TYPEALIASES_ONLY", "WRONG_MODIFIER_TARGET")
    private inner typealias A2 = B

    fun A1(x: Any) = x
    fun A2(x: Any) = x
}
