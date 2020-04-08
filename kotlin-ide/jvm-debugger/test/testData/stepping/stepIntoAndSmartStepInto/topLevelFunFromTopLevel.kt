package topLevelFunFromTopLevel

fun bar() {
    val a = 1
}

fun main(args: Array<String>) {
    //Breakpoint!
    bar()
}
