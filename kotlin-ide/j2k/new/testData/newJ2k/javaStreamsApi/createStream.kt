import java.util.Arrays
import java.util.stream.Collectors
import java.util.stream.Stream

internal class Test {
    fun main(lst: List<String>) {
        val streamOfList = lst.stream()
            .map { x: String -> x + "e" }
            .collect(Collectors.toList())
        val streamOfElements = Stream.of(1, 2, 3)
            .map { x: Int -> x + 1 }
            .collect(Collectors.toList())
        val array = arrayOf(1, 2, 3)
        val streamOfArray = Arrays.stream(array)
            .map { x: Int -> x + 1 }
            .collect(Collectors.toList())
        val streamOfArray2 = Stream.of(*array)
            .map { x: Int -> x + 1 }
            .collect(Collectors.toList())
        val streamIterate = Stream.iterate(2, { v: Int -> v * 2 })
            .map { x: Int -> x + 1 }
            .collect(Collectors.toList())
        val streamGenerate = Stream.generate { 42 }
            .map { x: Int -> x + 1 }
            .collect(Collectors.toList())
    }
}