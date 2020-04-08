package streams.collection.source

fun main(args: Array<String>) {
  val collection: Collection<Any> = listOf(1, 2, 3, 5)
  // Breakpoint!
  collection.count { it == 3 }
}