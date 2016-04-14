class Test {
    fun notNullString(p: Int): String {
        return "response"
    }

    private val notNullInitializerField = notNullString(3)
    var notNullInitializerPublicField = notNullString(3)

    fun testProperty() {
        notNullInitializerField[0]
        notNullInitializerPublicField[0]
    }

    fun testLocalVariable() {
        val notNullInitializerVal = notNullString(3)
        notNullInitializerVal[0]
    }
}