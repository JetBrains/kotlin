package kt17514

fun <T> checkSucceeds(callable: () -> T) = callable()

fun main(args: Array<String>) {
    val isPresent = true
    val isPresent2 = true
    val expectedLibs = listOf("aa", "bb", "cc")
    expectedLibs
            .forEach {
                checkSucceeds {
                    val isPresent2 = false
                    //Breakpoint!
                    val v = isPresent
                }
            }
}

// EXPRESSION: isPresent
// RESULT: 1: Z

// EXPRESSION: isPresent2
// RESULT: 0: Z