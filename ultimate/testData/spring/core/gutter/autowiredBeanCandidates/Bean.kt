import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean

class Bean {
    @Autowired
    fun setProp1(autowiredProp: String) {
    }

    @Autowired
    lateinit var prop2: String

    @Be<caret>an(name = "myBean")
    @Autowired
    fun myBean(collab: String): String {
        return "aaa" + collab
    }
}