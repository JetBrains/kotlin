// ERROR: Type mismatch: inferred type is String? but String was expected
class TestJava {

    var nullableInitializerFieldCast: String = nullableObj(3) as String?
    private val nullableInitializerPrivateFieldCast = nullableObj(3) as String?
    fun nullableObj(p: Int): Any? {
        return if (p > 0) "response" else null
    }

    fun testProperty() {
        nullableInitializerFieldCast[0]
        nullableInitializerPrivateFieldCast!![0]
    }

    fun testLocalVariable() {
        val nullableInitializerValCast = nullableObj(3) as String?

        nullableInitializerValCast!![0]
    }
}