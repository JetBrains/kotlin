import java.util.ArrayList
import java.util.function.Consumer

internal class Test {
    fun context() {
        val items: MutableList<Double> = ArrayList()
        items.add(1.0)
        items.forEach(Consumer { o: Double -> println(o) })
    }
}