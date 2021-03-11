package hideSyntheticThis

fun main() {
    block("foo") {
        //Breakpoint!
        val b = 4
    }
}

fun <T> block(t: T, block: T.() -> Unit) {
    t.block()
}

// PRINT_FRAME
// SHOW_KOTLIN_VARIABLES
// DESCRIPTOR_VIEW_OPTIONS: NAME_EXPRESSION