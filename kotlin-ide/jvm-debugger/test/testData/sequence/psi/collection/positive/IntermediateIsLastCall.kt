fun main(args: Array<String>) {
<caret>  listOf(1, 2, 3).map { it * it }.filter { it % 2 == 0 }
}