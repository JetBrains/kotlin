public class NonDefault() {
    class object {
        public fun main(args: Array<String>) {

            val value = 3
            var valueString = ""
            when (value) {
                1 -> {
                    valueString = "ONE"
                }
                2 -> {
                    valueString = "TWO"
                }
                3 -> {
                    valueString = "THREE"
                }
                else -> {
                }
            }
            System.out.println(valueString)
        }
    }
}
fun main(args: Array<String>) = NonDefault.main(args as Array<String?>?)