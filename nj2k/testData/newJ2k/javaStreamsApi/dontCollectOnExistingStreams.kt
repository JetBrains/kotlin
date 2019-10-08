import java.util.stream.Collectors
import java.util.stream.Stream

internal class Test {
    fun main(lst: List<String?>?) {
        val stream = Stream.of(1)
        val list = stream.collect(Collectors.toList())
    }
}