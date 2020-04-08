package stopInLocalFunInSecondaryConstructor

class Foo(bar: Any) {
    constructor() : this(12) {
        fun some() {
            //Breakpoint!
            nop()
            nop()
        }

        some()
    }

    fun nop() {}
}

fun main(args: Array<String>) {
    Foo()
}
