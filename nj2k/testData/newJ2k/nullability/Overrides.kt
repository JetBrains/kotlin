internal open class Base {
    open fun foo(s: String?): String? {
        return ""
    }

    open fun bar(s: String?): String? {
        return if (s != null) s + 1 else null
    }

    open fun zoo(o: Any?): String? {
        return ""
    }

    open fun nya(s: String): String {
        return s
    }
}

internal interface I {
    fun zoo(o: Any?): String?
    fun nya(s: String): String {
        return ""
    }
}

internal class C : Base(), I {
    override fun foo(s: String?): String? {
        return ""
    }

    override fun bar(s: String?): String? {
        return ""
    }

    override fun zoo(o: Any?): String? {
        return ""
    }

    override fun nya(s: String): String {
        return ""
    }
}
