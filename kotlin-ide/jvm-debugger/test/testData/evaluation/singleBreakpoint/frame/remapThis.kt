package remapThis

fun main() {
    Bar().bar()
}

class Bar {
    fun bar() {
        "a".foo()
    }

    fun String.foo() {
        //Breakpoint!
        val a = this
    }
}

// PRINT_FRAME
// SHOW_KOTLIN_VARIABLES
// DESCRIPTOR_VIEW_OPTIONS: NAME_EXPRESSION