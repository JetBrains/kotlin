import java.util.stream.Stream

fun main(args: Array<String>) {
  val before = 10
  val count = Stream.of("abc", "acd", "ef").map(String::length).count()<caret>
  val after = 20
}
