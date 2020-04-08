package streams.collection.filter

fun main(args: Array<String>) {
  // Breakpoint!
  listOf(Any(), Any(), Any()).filter { false }.count()
}