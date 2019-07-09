internal interface I {
    val string: String?
}

internal class C {
    fun foo(i: I) {
        if (i.string == null) {
            println("null")
        }
    }
}