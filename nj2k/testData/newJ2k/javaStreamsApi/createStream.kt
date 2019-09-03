import java.util.Arrays
import java.util.stream.Collectors
import java.util.stream.Stream

internal class Test {
    fun main(lst: List<String>) {
        val streamOfList: List<String> = lst.stream()
                .map { x: String -> x + "e" }
                .collect(Collectors.toList())
        val streamOfElements: List<Int> = Stream.of(1, 2, 3)
                .map { x: Int -> x + 1 }
                .collect(Collectors.toList())
        val array = arrayOf(1, 2, 3)
        val streamOfArray: List<Int> = Arrays.stream(array)
                .map { x: Int -> x + 1 }
                .collect(Collectors.toList())
        val streamOfArray2: List<Int> = Stream.of(*array)
                .map { x: Int -> x + 1 }
                .collect(Collectors.toList())
        val streamIterate: List<Int> = Stream.iterate(2, { v: Int -> v * 2 })
                .map { x: Int -> x + 1 }
                .collect(Collectors.toList())
        val streamGenerate: List<Int> = Stream.generate { 42 }
                .map { x: Int -> x + 1 }
                .collect(Collectors.toList())
    }
}