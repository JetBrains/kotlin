internal interface Getter {
    fun get(): String?
}

internal class C {
    fun foo(b: Boolean): String {
        val getter: Getter = object : Getter {
            override fun get(): String? {
                return null
            }
        }
        return ""
    }
}