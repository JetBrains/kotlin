package stopInInlineUnderOtherCall

fun main(args: Array<String>) {
    val a = 1

    nonInline(
            {
                inlineCall {
                    {
                        //Breakpoint!
                        foo(a)
                    }()
                }
            }
    )
}

fun foo(a: Any) {}

fun nonInline(f: () -> Unit) {
    f()
}

inline fun inlineCall(f: () -> Unit) {
    f()
}