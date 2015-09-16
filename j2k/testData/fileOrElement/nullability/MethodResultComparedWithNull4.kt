internal interface I {
    fun getString(): String
}

internal class C {
    fun foo(i: I, b: Boolean) {
        var result: String? = i.getString()
        if (b) result = null
        if (result != null) {
            print(result)
        }
    }
}