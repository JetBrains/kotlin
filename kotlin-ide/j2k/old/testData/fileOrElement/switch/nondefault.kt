object NonDefault {
    @JvmStatic
    fun main(args: Array<String>) {

        val value = 3
        var valueString = ""
        when (value) {
            1 -> valueString = "ONE"
            2 -> valueString = "TWO"
            3 -> valueString = "THREE"
        }
        println(valueString)
    }
}