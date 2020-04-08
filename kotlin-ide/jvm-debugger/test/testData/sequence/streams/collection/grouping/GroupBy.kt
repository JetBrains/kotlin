package streams.collection.grouping

fun main(args: Array<String>) {
  // Breakpoint!
  listOf(1, 2, 3, 4, 5).groupBy { it % 2 }
}