// ERROR: Type inference failed. Expected type mismatch: found: java.util.HashMap<kotlin.Any!, kotlin.Any!> required: kotlin.Map<kotlin.String, kotlin.String>
import java.util.*

class A {
    class object {
        public fun foo(): Map<String, String> {
            val props = Properties()
            return HashMap(props as Map<Any, Any>)
        }
    }
}