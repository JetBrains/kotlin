import java.util.stream.Stream

fun main(args: Array<String>) {
  <caret> Stream.of(Any()).map({ 5 }).count()
}