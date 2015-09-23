// ERROR: Type mismatch: inferred type is kotlin.String? but kotlin.Any was expected
internal class A {
    private val s: String? = null

    val value: Any
        get() = s
}