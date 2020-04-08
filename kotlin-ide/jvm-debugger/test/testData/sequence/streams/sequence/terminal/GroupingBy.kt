package streams.sequence.terminal

fun main(args: Array<String>) {
  //Breakpoint!
  sequenceOf(1, 2, 3, 4, 5).groupingBy { it % 2 }
}