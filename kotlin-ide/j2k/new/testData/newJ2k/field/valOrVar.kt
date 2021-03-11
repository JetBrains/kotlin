internal class A(private val field6: Int, private val field8: Int, a: A) {
    private val field1 = 0
    private val field2 = 0
    private var field3 = 0
    val field4 = 0
    var field5 = 0
    private val field7 = 10
    private val field9 = 10
    private var field10 = 0
    private var field11 = 0
    fun foo() {
        field3 = field2
    }

    init {
        if (field6 > 0) {
            field10 = 10
        }
        a.field11 = 10
    }
}