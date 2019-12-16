// ERROR: Type mismatch: inferred type is (Any?..Any?) but String was expected
// ERROR: Type mismatch: inferred type is (Any?..Any?) but String was expected
// ERROR: Type mismatch: inferred type is HashMap<String, Any?> but Map<String, String> was expected
import java.util.*

internal object A {
    fun foo(): Map<String, String> {
        val props = Properties()
        return HashMap(props as Map<*, *>)
    }
}