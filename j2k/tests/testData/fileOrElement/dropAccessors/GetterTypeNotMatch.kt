// ERROR: Type mismatch: inferred type is kotlin.String? but kotlin.Any was expected
class A {
    private val s: String? = null

    public fun getValue(): Any {
        return s
    }
}
