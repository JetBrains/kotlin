package streams.sequence.map

fun main(args: Array<String>) {
  //Breakpoint!
  doubleArrayOf(1.0, 2.0).asSequence().map { it * it }.contains(3.0)
}