class TestJava {

    var nullableInitializerFieldCast = nullableObj(3) as String?
    private val nullableInitializerPrivateFieldCast = nullableObj(3) as String?
    fun nullableObj(p: Int): Any? {
        return if (p > 0) "response" else null
    }

    fun testProperty() {
        nullableInitializerFieldCast!![0]
        nullableInitializerPrivateFieldCast!![0]
    }

    fun testLocalVariable() {
        val nullableInitializerValCast = nullableObj(3) as String?

        nullableInitializerValCast!![0]
    }
}