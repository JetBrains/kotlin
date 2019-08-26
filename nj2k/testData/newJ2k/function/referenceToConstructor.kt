import java.util.Arrays
import java.util.stream.Collectors

object TestLambda {
    @JvmStatic
    fun main(args: Array<String>) {
        val names: List<String> = Arrays.asList("A", "B")
        val people: List<Person?> = names.stream().map { name: String -> Person(name) }.collect(Collectors.toList())
    }
}
