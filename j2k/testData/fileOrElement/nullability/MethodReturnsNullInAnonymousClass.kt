// ERROR: Return type of 'get' is not a subtype of the return type of overridden member public abstract fun get(): kotlin.String defined in Getter
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