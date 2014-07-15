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