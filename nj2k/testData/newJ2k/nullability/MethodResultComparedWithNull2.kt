internal interface I {
    val string: String?
}

internal class C {
    fun foo(i: I) {
        val result = i.string
        result?.let { print(it) }
    }
}