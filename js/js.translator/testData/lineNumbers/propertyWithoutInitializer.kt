open class A {
    val x: Int

    open val y: Int

    init {
        x = 23
        y = 42
    }
}

// LINES(JS):      1 2 4 7 7 8 8       4 4 4
// LINES(JS_IR): 1 1     7 7 8 8 2 2 2 4 4 4
