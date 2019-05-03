// ERROR: Type inference failed: Not enough information to infer parameter K in constructor HashMap<K : Any!, V : Any!>() Please specify it explicitly.
// ERROR: Type inference failed: Not enough information to infer parameter K in constructor HashMap<K : Any!, V : Any!>() Please specify it explicitly.
import java.util.HashMap

internal class G<T : String>(t: T)

class Java {
    internal fun test() {
        val m = HashMap()
        m.put(1, 1)
    }

    internal fun test2() {
        val m = HashMap()
        val g = G("")
        val g2 = G("")
    }
}