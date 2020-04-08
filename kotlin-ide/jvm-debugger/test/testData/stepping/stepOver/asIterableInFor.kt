package asIterableInFor

fun <T> nonInline(p: T): T = p

fun main(args: Array<String>) {
    //Breakpoint!
    val list = listOf("a", "b", "c")
    for (element in list.asIterable()) {
        nonInline(element)
    }
}

// STEP_OVER: 4

// See KT-13534.
