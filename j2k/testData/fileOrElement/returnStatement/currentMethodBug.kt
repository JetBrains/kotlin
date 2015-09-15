internal interface I {
    fun getInt(): Int
}

internal class C {
    internal fun getObject(): Any? {
        foo(object : I {
            override fun getInt(): Int {
                return 0
            }
        })
        return string
    }

    internal fun foo(i: I) {
    }

    internal var string: String? = null
}