package switch_demo

object SwitchDemo {
    @JvmStatic
    fun main(args: Array<String>) {
        val month = 8
        val monthString: String
        monthString = when (month) {
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 -> "December"
            else -> "Invalid month"
        }
        println(monthString)
    }
}