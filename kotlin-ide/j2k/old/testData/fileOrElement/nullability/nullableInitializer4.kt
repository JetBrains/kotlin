// ERROR: Type mismatch: inferred type is String? but String was expected
class TestJava {
    private var notNullInitializerFieldNullableUsage = "aaa"
    private var notNullInitializerFieldNotNullUsage = "aaa"

    private var nullInitializerFieldNullableUsage: String? = null
    private var nullInitializerFieldNotNullUsage: String? = null

    fun testNotNull(obj: Any?) {
        if (true) {
            notNullInitializerFieldNullableUsage = obj as String?
            notNullInitializerFieldNotNullUsage = "str"

            notNullInitializerFieldNullableUsage[1]
            notNullInitializerFieldNotNullUsage[1]
        } else {
            nullInitializerFieldNullableUsage = obj as String?
            nullInitializerFieldNotNullUsage = "str"

            nullInitializerFieldNullableUsage!![1]
            nullInitializerFieldNotNullUsage!![1]
        }
    }
}