package stopInlineCallInLocalFunInSecondaryConstructor

class Foo(bar: Any) {
    constructor() : this(12) {
        fun test() {
            inlineFun {
                //Breakpoint!
                nop()
                nop()
            }
        }

        test()
    }

    fun nop() {}
}

fun main(args: Array<String>) {
    Foo()
}

inline fun inlineFun(f: () -> Any) {
    f()
}