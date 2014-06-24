import java.util.ArrayList

class Boxing() {
    fun test() {
        val i: Int? = 0
        val n = 0.0.toFloat()
        i = 1
        val j = i!!
        val k = i!! + 2
        i = null
        j = i!!
    }
}