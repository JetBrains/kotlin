import java.util.*
import kotlin.List

class A {
    private val field1 = ArrayList<String>()
    val field2: List<String> = ArrayList()
    public val field3: Int = 0
    protected val field4: Int = 0

    private var field5: List<String> = ArrayList()
    var field6: List<String> = ArrayList()

    private var field7 = 0
    var field8 = 0

    private var field9: String? = "a"
    private var field10: String? = foo()

    fun foo(): String {
        return "x"
    }

    fun bar() {
        field5 = ArrayList<String>()
        field7++
        field8++
        field9 = null
        field10 = null
    }
}