// ERROR: Type mismatch: inferred type is String? but Any was expected
internal class A {
    private val s: String? = null

    val value: Any
        get() = s
}