import java.util.ArrayList

class Boxing {
    fun test() {
        var i: Int? = 0
        val n = 0.0.toFloat()
        i = 1
        var j = i!!
        val k = i!! + 2
        i = null
        j = i!!
    }
}