// !specifyFieldTypeByDefault: true
import java.util.*
import kotlin.List

class A() {
    private val field1: List<String> = ArrayList<String>()
    val field2: List<String> = ArrayList<String>()
    public val field3: Int = 0
    protected val field4: Int = 0

    private var field5: List<String> = ArrayList<String>()
    var field6: List<String> = ArrayList<String>()

    private var field7: Int = 0
    var field8: Int = 0
}