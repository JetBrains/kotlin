// ERROR: Type mismatch: inferred type is Any? but String? was expected
// ERROR: Type mismatch: inferred type is (CapturedType(*)!!..Any?) but String? was expected
// ERROR: Type mismatch: inferred type is Any? but String? was expected
// ERROR: Type mismatch: inferred type is (CapturedType(*)!!..Any?) but String? was expected
// ERROR: Type mismatch: inferred type is kotlin.collections.HashMap<Any?, Any?> /* = java.util.HashMap<Any?, Any?> */ but Map<String?, String?> was expected
import java.util.Properties

internal object A {
    fun foo(): Map<String?, String?> {
        val props = Properties()
        return HashMap<Any?, Any?>(props as Map<*, *>)
    }
}