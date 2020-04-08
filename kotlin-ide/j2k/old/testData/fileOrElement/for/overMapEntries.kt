import java.util.HashMap

internal object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        val resultMap = HashMap<String, String>()
        for ((key, type) in resultMap) {

            if (key == "myKey") {
                println(type)
            }
        }
    }
}