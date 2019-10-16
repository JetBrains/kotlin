import java.util.stream.Collectors

internal class Test {
    fun main(lst: List<String>) {
        val toList = lst.stream().collect(Collectors.toList())
        val toSet = lst.stream().collect(Collectors.toSet())
        val count = lst.stream().count()
        val anyMatch = lst.stream().anyMatch { v: String -> v.isEmpty() }
        val allMatch = lst.stream().allMatch { v: String -> v.isEmpty() }
        val noneMatch = lst.stream().noneMatch { v: String -> v.isEmpty() }
        lst.stream().forEach { v: String? -> println(v) }
    }
}