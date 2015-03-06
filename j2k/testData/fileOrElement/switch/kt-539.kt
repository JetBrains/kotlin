package switch_demo

public class SwitchDemo {
    default object {
        public fun main(args: Array<String>) {

            val month = 8
            val monthString: String
            when (month) {
                1 -> monthString = "January"
                2 -> monthString = "February"
                3 -> monthString = "March"
                4 -> monthString = "April"
                5 -> monthString = "May"
                6 -> monthString = "June"
                7 -> monthString = "July"
                8 -> monthString = "August"
                9 -> monthString = "September"
                10 -> monthString = "October"
                11 -> monthString = "November"
                12 -> monthString = "December"
                else -> monthString = "Invalid month"
            }
            System.out.println(monthString)
        }
    }
}

fun main(args: Array<String>) = SwitchDemo.main(args)