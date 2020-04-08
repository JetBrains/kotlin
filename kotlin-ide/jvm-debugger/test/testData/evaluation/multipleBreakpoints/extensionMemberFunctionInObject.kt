package extensionMemberFunctionInObject

// KT-14822

fun main(args: Array<String>) {
    Foo.bar()
}

object Foo {
    fun bar() {
        // EXPRESSION: "OK".baz()
        // RESULT: 1: I
        //Breakpoint!
        "OK".baz()

        with("OK") {
            // EXPRESSION: baz()
            // RESULT: 1: I
            //Breakpoint!
            baz()
        }
    }

    fun String.baz(): Int {
        return 1
    }
}