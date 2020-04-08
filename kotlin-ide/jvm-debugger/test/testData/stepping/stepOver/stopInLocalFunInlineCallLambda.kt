package stopInLocalFunInlineCallLambda

fun main(args: Array<String>) {
    var prop = 1
    fun local() {
        inlineFun {
            {
                //Breakpoint!
                foo(12)
            }()
        }
    }

    local()
}

inline fun inlineFun(f: () -> Unit) {
    f()
}

fun foo(a: Any) {}