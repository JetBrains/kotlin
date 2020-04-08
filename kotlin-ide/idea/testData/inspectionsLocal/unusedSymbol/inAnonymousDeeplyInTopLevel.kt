// PROBLEM: none
// WITH_RUNTIME

private val xs = listOf(1, 2, 3).flatMap { x ->
    listOf(3, 4, 5).map { y ->
        object {
            val <caret>value = x + y
        }
    }
}

fun foo() {
    xs.forEach {
        println("value: " + it.value)
    }
}