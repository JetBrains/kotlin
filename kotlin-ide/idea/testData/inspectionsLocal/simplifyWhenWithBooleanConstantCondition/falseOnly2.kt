// ERROR: 'when' expression must be exhaustive, add necessary 'else' branch
// WITH_RUNTIME
fun test() {
    val x = <caret>when {
        false -> {
            println(1)
            1
        }
    }
}