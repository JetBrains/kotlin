import java.util.stream.Collectors

internal class Test {
    fun main(lst: List<String>) {
        val toList: List<String> = lst.stream().collect(Collectors.toList())
        val toSet: Set<String> = lst.stream().collect(Collectors.toSet())
        val count = lst.stream().count()
        val anyMatch = lst.stream().anyMatch { v: String -> v.isEmpty() }
        val allMatch = lst.stream().allMatch { v: String -> v.isEmpty() }
        val noneMatch = lst.stream().noneMatch { v: String -> v.isEmpty() }
        lst.stream().forEach { v: String -> println(v) }
    }
}