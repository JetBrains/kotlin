import java.util.stream.Stream

fun main(args: Array<String>) {
<caret>  Stream.of(1, 2, 3)
      .mapToInt { it }
      .mapToObj({ it.toString() })
      .mapToLong { it.toLong() }
      .mapToDouble { it / 10.0 }
      .count()
}