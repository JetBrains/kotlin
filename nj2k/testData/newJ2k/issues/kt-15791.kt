import java.util.ArrayList

internal object A {
    @JvmStatic
    fun main(args: Array<String>) {
        List::class.java.isAssignableFrom(ArrayList::class.java)
    }
}