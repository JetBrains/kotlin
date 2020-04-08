import java.util.stream.IntStream

fun foo(baz: Int, bar: Int): Int {
  return IntStream.range(1, 2).sum()
}

fun main(args: Array<String>) {
<caret>  foo(0, IntStream.of(1, 2, 3).sum())
}
