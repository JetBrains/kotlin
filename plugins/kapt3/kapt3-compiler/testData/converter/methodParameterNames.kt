// FIR_BLOCKED: LC don't support DefaultImpls
interface Intf {
    fun foo(abc: String)

    fun bar(bcd: Int): String {
        return ""
    }
}

abstract class Cls {
    abstract fun foo(abc: String)

    fun bar(bcd: Int): String {
        return ""
    }
}
