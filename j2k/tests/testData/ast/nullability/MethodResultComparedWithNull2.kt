trait I {
    public fun getString(): String?
}

class C {
    fun foo(i: I) {
        val result = i.getString()
        if (result != null) {
            print(result)
        }
    }
}