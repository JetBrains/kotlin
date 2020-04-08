package streams.sequence.distinct

fun main(args: Array<String>) {
  //Breakpoint!
  listOf(3, 4, 234, 34, 54, 23, 4, 23, 543, 5, 46).asSequence().distinctBy { it % 10 }.toList()
}