// ERROR: Type inference failed. Expected type mismatch: inferred type is HashMap<Any?, Any?> but Map<String, String> was expected
import java.util.*

internal object A {
    fun foo(): Map<String, String> {
        val props = Properties()
        return HashMap(props as Map<*, *>)
    }
}