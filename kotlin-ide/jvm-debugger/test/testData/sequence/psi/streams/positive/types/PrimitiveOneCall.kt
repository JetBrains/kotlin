import java.util.stream.IntStream

fun main(args: Array<String>) {
  <caret>  IntStream.of(1, 3, 4).count()
}