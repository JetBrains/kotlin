package inlineFunThisKotlinVariables

class Same {
    fun useInline() {
        Different().inlineFun()
    }
}

class Different {
    inline fun inlineFun() {
        //Breakpoint!
        println(1)
    }
}

fun main() {
    Same().useInline()
}

// PRINT_FRAME
// SHOW_KOTLIN_VARIABLES