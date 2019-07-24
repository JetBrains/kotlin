import java.util.stream.Collectors
import java.util.stream.Stream

internal class Test {
    fun main(lst: List<String?>?) {
        val stream: Stream<Int> = Stream.of(1)
        val list: List<Int> = stream.collect(Collectors.toList())
    }
}