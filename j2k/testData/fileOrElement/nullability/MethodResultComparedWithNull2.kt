internal interface I {
    val string: String?
}

internal class C {
    fun foo(i: I) {
        val result = i.string
        if (result != null) {
            print(result)
        }
    }
}