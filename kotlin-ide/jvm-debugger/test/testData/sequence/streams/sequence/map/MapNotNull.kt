package streams.sequence.map

fun main(args: Array<String>) {
  //Breakpoint!
  val lst = listOf(1, 2, null, 3).asSequence().mapNotNull { if (it != null && it % 2 == 1) it else null }.toList()
}