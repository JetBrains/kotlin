import java.io.IOException

class A {
    fun foo() {
        try {
            bar()
        } catch (e: RuntimeException) {
            e.printStackTrace() // print stack trace
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
}