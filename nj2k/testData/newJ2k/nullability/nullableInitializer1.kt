class Test {

    private var nullableInitializerField = nullableString(3)
    private val nullableInitializerFieldFinal = nullableString(3)
    var nullableInitializerPublicField = nullableString(3)
    fun nullableString(p: Int): String? {
        return if (p > 0) "response" else null
    }

    fun testProperty() {
        nullableInitializerField = "aaa"

        nullableInitializerField!![0]
        nullableInitializerFieldFinal!![0]
        nullableInitializerPublicField!![0]
    }

    fun testLocalVariable() {
        val nullableInitializerVal = nullableString(3)
        nullableInitializerVal!![0]
    }
}