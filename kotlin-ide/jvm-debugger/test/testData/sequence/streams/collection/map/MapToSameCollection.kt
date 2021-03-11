package streams.collection.map

fun main(args: Array<String>) {
  val dst = mutableListOf(1, 2, 3)
  // Breakpoint!
  listOf(4, 5, 6).mapTo(dst, { it * it }).filter { it % 4 != 0 }.mapTo(dst, { it * it })
  println(dst)
}