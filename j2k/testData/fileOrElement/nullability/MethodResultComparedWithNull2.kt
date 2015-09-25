internal interface I {
    fun getString(): String?
}

internal class C {
    fun foo(i: I) {
        val result = i.getString()
        if (result != null) {
            print(result)
        }
    }
}