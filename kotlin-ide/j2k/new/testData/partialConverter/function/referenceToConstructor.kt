import java.util.Arrays
import java.util.stream.Collectors

object TestLambda {
    @JvmStatic
    fun main(args: Array<String>) {
        val names = Arrays.asList("A", "B")
        val people = names.stream().map { name: String? -> Person(name) }.collect(Collectors.toList())
    }
}