class Test {

    private val notNullInitializerField = notNullString(3)
    var notNullInitializerPublicField = notNullString(3)
    fun notNullString(p: Int): String {
        return "response"
    }

    fun testProperty() {
        notNullInitializerField[0]
        notNullInitializerPublicField[0]
    }

    fun testLocalVariable() {
        val notNullInitializerVal = notNullString(3)
        notNullInitializerVal[0]
    }
}