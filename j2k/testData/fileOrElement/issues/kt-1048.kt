// ERROR: Type inference failed: Not enough information to infer parameter K in constructor HashMap<K : kotlin.Any!, V : kotlin.Any!>() Please specify it explicitly.
// ERROR: Type inference failed: Not enough information to infer parameter K in constructor HashMap<K : kotlin.Any!, V : kotlin.Any!>() Please specify it explicitly.
import java.util.HashMap

class G<T : String>(t: T)

public class Java {
    fun test() {
        val m = HashMap()
        m.put(1, 1)
    }

    fun test2() {
        val m = HashMap()
        val g = G("")
        val g2 = G("")
    }
}