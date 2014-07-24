class A(private val field6: Int, private val field8: Int, a: A) {
    private val field1 = 0
    private val field2 = 0
    private var field3 = 0
    val field4 = 0
    var field5 = 0
    private val field7: Int
    private val field9: Int
    private var field10: Int = 0
    private var field11: Int = 0

    {
        field7 = 10
        this.field9 = 10
        if (field6 > 0) {
            this.field10 = 10
        }
        a.field11 = 10
    }

    fun foo() {
        field3 = field2
    }
}