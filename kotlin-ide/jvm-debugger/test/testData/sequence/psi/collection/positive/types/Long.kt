fun main(args: Array<String>) {
  longArrayOf(1L, 2L<caret>).count { it < 2 }
}