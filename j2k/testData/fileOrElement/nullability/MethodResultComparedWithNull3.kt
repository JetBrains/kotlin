internal interface I {
    fun getString(): String?
}

internal class C {
    internal fun foo(i: I) {
        val result = i.getString()
        if (result != null) {
            print(result)
        }
    }
}