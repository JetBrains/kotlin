trait I {
    public fun getString(): String?
}

class C {
    fun foo(i: I) {
        if (i.getString() == null) {
            println("null")
        }
    }
}