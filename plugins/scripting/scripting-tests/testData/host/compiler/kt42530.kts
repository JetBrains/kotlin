
fun Int.isOdd() = (this % 2) == 1
val list: List<Pair<Int, String>> = listOf(1 to "a", 2 to "b")
val (odds, evens) = list.partition { (i, _) -> i.isOdd() }
println(odds)
odds
