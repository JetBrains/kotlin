import java.util.*

internal object A {
    fun foo(): Map<String?, String?> {
        val props = Properties()
        return HashMap<Any?, Any?>(props as Map<*, *>)
    }
}