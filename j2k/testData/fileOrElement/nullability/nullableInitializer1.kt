// ERROR: Type mismatch: inferred type is String? but String was expected
// ERROR: Type mismatch: inferred type is String? but String was expected
class Test {
    fun nullableString(p: Int): String? {
        return if (p > 0) "response" else null
    }

    private var nullableInitializerField: String = nullableString(3)
    private val nullableInitializerFieldFinal = nullableString(3)
    var nullableInitializerPublicField: String = nullableString(3)

    fun testProperty() {
        nullableInitializerField = "aaa"

        nullableInitializerField[0]
        nullableInitializerFieldFinal!![0]
        nullableInitializerPublicField[0]
    }

    fun testLocalVariable() {
        val nullableInitializerVal = nullableString(3)
        nullableInitializerVal!![0]
    }
}