import java.util.stream.Stream

fun main(args: Array<String>) {
  val bef<caret>ore = 10
  val count = Stream.of("abc", "acd", "ef").map(String::length).count()
}
