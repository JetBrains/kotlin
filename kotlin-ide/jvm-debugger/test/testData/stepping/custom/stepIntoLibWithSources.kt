package stepIntoLibWithSources

import java.io.StringReader

fun main() {
    //Breakpoint!
    val text = StringReader("OK").readText()
}

// SMART_STEP_INTO_BY_INDEX: 2