import java.util.stream.Stream

fun main(args: Array<String>) {
  Stream.of(1)
      .peek { x -> <caret>Stream.of(1).count() }.forEach { }
}
