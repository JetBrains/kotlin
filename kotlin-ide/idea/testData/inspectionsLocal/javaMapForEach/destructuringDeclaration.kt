// PROBLEM: none
// RUNTIME_WITH_FULL_JDK
fun test(map: Map<Int, String>) {
    val mapOfPairs = mapOf<Pair<Int, Int>, Int>()
    mapOfPairs.<caret>forEach { (first, second), value ->
        println(first)
        println(second)
        println(value)
    }
}