// HIGHLIGHT: INFORMATION
// WITH_RUNTIME

fun test(list: List<Int>): List<Int> {
    return list
            .filter<caret> { it > 1 }
            .map { it * 2 }
            .map { it * 3 }
            .map { it * 4 }
}