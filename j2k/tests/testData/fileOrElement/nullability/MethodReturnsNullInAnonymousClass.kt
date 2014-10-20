trait Getter {
    public fun get(): String
}

class C {
    fun foo(b: Boolean): String {
        val getter = object : Getter {
            override fun get(): String? {
                return null
            }
        }
        return ""
    }
}