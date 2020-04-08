package finallyBlock

fun throwException() { throw RuntimeException() }
fun foo() {}

fun main(args: Array<String>) {
    fun wrap(f: () -> Unit) = try { f() } catch (e: Throwable) {}

    wrap(::test1)
    wrap(::test2)
    wrap(::test3)
}

fun test1() {
    try {
        //Breakpoint!
        throwException()
    } finally {
        //Breakpoint!
        foo()
    }
}

fun test2() {
    try {
        //Breakpoint!
        foo()
    } finally {
        //Breakpoint!
        foo()
    }
}

fun test3() {
    try {
        //Breakpoint!
        throwException()
    } finally {
        //Breakpoint!
        try {
            throwException()
        } finally {
            //Breakpoint!
            foo()
        }
    }
}

// RESUME: 7