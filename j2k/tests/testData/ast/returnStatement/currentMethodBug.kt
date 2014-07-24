trait I {
    public fun getInt(): Int
}

class C {
    fun getObject(): Any? {
        foo(object : I {
            override fun getInt(): Int {
                return 0
            }
        })
        return string
    }

    fun foo(i: I) {
    }

    var string: String? = null
}