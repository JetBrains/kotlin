package switch_demo

public class SwitchDemo {
    default object {
        public fun main(args: Array<String>) {
            val month = 8
            val monthString: String
            when (month) {
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 -> monthString = "December"
                else -> monthString = "Invalid month"
            }
            System.out.println(monthString)
        }
    }
}

fun main(args: Array<String>) = SwitchDemo.main(args)