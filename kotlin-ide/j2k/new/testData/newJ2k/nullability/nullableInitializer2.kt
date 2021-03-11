class TestJava {
    fun nullableObj(p: Int): Any? {
        return if (p > 0) "response" else null
    }

    var nullableInitializerFieldCast = nullableObj(3) as String?
    private val nullableInitializerPrivateFieldCast = nullableObj(3) as String?
    fun testProperty() {
        nullableInitializerFieldCast!![0]
        nullableInitializerPrivateFieldCast!![0]
    }

    fun testLocalVariable() {
        val nullableInitializerValCast = nullableObj(3) as String?
        nullableInitializerValCast!![0]
    }
}