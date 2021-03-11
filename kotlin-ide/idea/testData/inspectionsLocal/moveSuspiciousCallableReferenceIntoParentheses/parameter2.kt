// WITH_RUNTIME

fun foo() {
    listOf(1,2,3).map { bar -> bar::toString }
    listOf(4,5).map {<caret> bar -> bar::toString }
}