open class A {
    open var x = 23

    var y: String = "42"
        get() = field +
                "!"
        set(value) {
            print(
                    field)
            field =
                    value.removeSuffix("!")
        }
}

// LINES(JS_IR): 1 1 2 2 4 4 2 2 2 2 2 2 7 8 9 10 10 11 5 6 6 5 6
