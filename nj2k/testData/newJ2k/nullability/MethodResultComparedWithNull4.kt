internal interface I {
    val string: String?
}

internal class C {
    fun foo(i: I, b: Boolean) {
        var result = i.string
        if (b) result = null
        if (result != null) {
            print(result)
        }
    }
}