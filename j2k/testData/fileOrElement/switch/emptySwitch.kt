public class NonDefault {
    default object {
        public fun main(args: Array<String>) {

            val value = 3
            val valueString = ""
            when (value) {

            }
            System.out.println(valueString)
        }
    }
}

fun main(args: Array<String>) = NonDefault.main(args)