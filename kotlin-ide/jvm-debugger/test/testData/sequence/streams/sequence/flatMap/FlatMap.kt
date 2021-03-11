package streams.sequence.flatMap

fun main(args: Array<String>) {
  //Breakpoint!
  listOf(1, 0, 2).asSequence().flatMap { (0 until it).asSequence() }.count()
}