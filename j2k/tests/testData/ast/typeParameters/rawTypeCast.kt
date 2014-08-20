import java.util.*
import kotlin.Map

class A {
    class object {
        public fun foo(): Map<String, String> {
            val props = Properties()
            return HashMap(props as Map<Any, Any>)
        }
    }
}