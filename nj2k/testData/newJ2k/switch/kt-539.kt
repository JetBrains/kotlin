package switch_demo

object SwitchDemo {
    @JvmStatic
    fun main(args: Array<String>) {

        val month = 8
        val monthString: String
        monthString = when (month) {
            1 -> "January"
            2 -> "February"
            3 -> "March"
            4 -> "April"
            5 -> "May"
            6 -> "June"
            7 -> "July"
            8 -> "August"
            9 -> "September"
            10 -> "October"
            11 -> "November"
            12 -> "December"
            else -> "Invalid month"
        }
        println(monthString)
    }
}