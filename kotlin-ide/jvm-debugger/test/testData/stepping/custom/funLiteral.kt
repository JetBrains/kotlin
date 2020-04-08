package funLiteral

fun main(args: Array<String>) {
    //Breakpoint!
    f1() {
        f2()
    }
}

fun f1(f: () -> Unit) { f() }
fun f2() {}

// SMART_STEP_INTO_BY_INDEX: 2