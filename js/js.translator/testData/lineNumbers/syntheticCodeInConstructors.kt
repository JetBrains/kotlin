object O {
    init {
        println()
    }
}

open class A(x: Int)

class B :
        A(
                23
        )

class C : A {
    constructor(x: Int) :
        super(
                x
        ) {
        println()
    }

    constructor() :
            this (
                    42
            )
}

// LINES: 1 3 * 1 1 1 * 10 11 * 15 16 17 14 19 15 22 23 24 22