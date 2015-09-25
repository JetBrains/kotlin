internal interface I {
    fun getString(): String?
}

internal class C {
    fun foo(i: I) {
        if (i.getString() == null) {
            println("null")
        }
    }
}