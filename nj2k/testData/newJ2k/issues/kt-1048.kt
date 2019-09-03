// ERROR: The integer literal does not conform to the expected type CapturedType(*)
// ERROR: The integer literal does not conform to the expected type CapturedType(*)
// ERROR: Type argument is not within its bounds: should be subtype of 'String?'
import java.util.HashMap

internal class G<T : String?>(t: T)
class Java {
    internal fun test() {
        val m: HashMap<*, *> = HashMap<Any?, Any?>()
        m[1] = 1
    }

    internal fun test2() {
        val m: HashMap<*, *> = HashMap<Any?, Any?>()
        val g: G<*> = G<Any?>("")
        val g2 = G("")
    }
}