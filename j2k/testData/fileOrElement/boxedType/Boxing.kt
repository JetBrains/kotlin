import java.util.ArrayList

internal class Boxing {
    internal fun test() {
        var i: Int? = 0
        val n = 0.0f
        i = 1
        var j = i
        val k = i + 2
        i = null
        j = i!!
    }
}