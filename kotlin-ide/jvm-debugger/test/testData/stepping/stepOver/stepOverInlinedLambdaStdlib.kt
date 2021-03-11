package stepOverInlinedLambdaStdlib

fun main(args: Array<String>) {
    //Breakpoint!
    val a = listOf(1, 2, 3)
    a.filter { it > 1 }                   /*!*/

    a.filter { it > 1 }.map { it * 2 }    /*!*/

    a.filter {                            /*!*/
        it > 1
    }.map {
        it * 2
    }
}                                         /*!*/

// TRACING_FILTERS_ENABLED: false
// STEP_OVER: 4