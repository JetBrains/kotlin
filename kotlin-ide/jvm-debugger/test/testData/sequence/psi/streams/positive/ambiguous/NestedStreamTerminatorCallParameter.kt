import java.util.stream.IntStream

fun main(args: Array<String>) {
<caret>  IntStream.of(1, 2, 3)
      .reduce(IntStream.of(1, 2)
          .reduce(IntStream.of(1, 2).sum()
          ) { left, right -> left + right }
      ) { left, right -> left + right }
}
