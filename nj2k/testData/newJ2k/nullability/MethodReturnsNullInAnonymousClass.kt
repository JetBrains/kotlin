// ERROR: Return type of 'get' is not a subtype of the return type of the overridden member 'public abstract fun get(): String defined in Getter'
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