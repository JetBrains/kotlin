package streams.sequence.misc

fun main(args: Array<String>) {
  //Breakpoint!
  sequenceOf(1, 2, 3, 2, 1).zipWithNext { prev, next -> next + prev }.count()
}