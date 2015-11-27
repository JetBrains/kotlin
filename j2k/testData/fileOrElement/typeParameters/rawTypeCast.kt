// ERROR: Type inference failed. Expected type mismatch: inferred type is java.util.HashMap<kotlin.Any!, kotlin.Any!> but kotlin.collections.Map<kotlin.String, kotlin.String> was expected
import java.util.*

internal object A {
    fun foo(): Map<String, String> {
        val props = Properties()
        return HashMap(props as Map<Any, Any>)
    }
}