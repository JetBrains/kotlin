open class Base {
    public open fun foo(s: String?): String? {
        return ""
    }

    public open fun bar(s: String?): String? {
        return if (s != null) s + 1 else null
    }

    public open fun zoo(o: Any): String {
        return ""
    }
}

trait I {
    public fun zoo(o: Any?): String?
}

class C : Base(), I {
    override fun foo(s: String?): String? {
        return ""
    }

    override fun bar(s: String?): String? {
        return ""
    }

    override fun zoo(o: Any?): String? {
        return ""
    }
}
