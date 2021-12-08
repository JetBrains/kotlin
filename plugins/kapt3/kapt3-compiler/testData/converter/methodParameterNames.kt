// IGNORE_BACKEND: JVM_IR

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
