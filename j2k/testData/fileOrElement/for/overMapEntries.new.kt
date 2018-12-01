internal object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        val resultMap: Map<String?, String?> = HashMap()
        for ((key, type) in resultMap) {
            if (key == "myKey") {
                println(type)
            }
        }
    }
}