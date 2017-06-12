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

// LINES: 2 4 2 2 * 5 6 8 9 10 11