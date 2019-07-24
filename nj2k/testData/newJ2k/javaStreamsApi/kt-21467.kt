import java.util.stream.Collectors
import java.util.stream.Stream

internal class Test {
    fun main() {
        val activities: List<String> = Stream.of("12")
                .map { v: String -> v + "nya" }
                .filter { v: String? -> v != null }
                .flatMap { v: String ->
                    Stream.of(v)
                            .flatMap { s: String -> Stream.of(s) }
                }.filter { v: String ->
                    val name: String = v.javaClass.name
                    if (name == "name") {
                        return@filter false
                    }
                    name != "other_name"
                }
                .collect(Collectors.toList())
    }
}