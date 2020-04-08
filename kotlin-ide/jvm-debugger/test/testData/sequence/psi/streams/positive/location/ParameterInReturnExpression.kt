import java.util.stream.IntStream

internal fun bar1(a: Int, b: Int, c: Int): Int {
  return 0
}

internal fun foo1(): Int {
<caret>  return bar1(0, 1, c = IntStream.range(1, 2).sum())
}

fun main(args: Array<String>) {
  foo1()
}
