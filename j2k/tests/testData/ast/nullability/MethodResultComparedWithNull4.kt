trait I {
    public fun getString(): String
}

class C {
    fun foo(i: I, b: Boolean) {
        var result: String? = i.getString()
        if (b) result = null
        if (result != null) {
            print(result)
        }
    }
}