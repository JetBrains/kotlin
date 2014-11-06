public class X {
    fun foo() {
        val runnable = object : Runnable {
            var value = 10

            override fun run() {
                System.out.println(value)
            }
        }
    }
}
