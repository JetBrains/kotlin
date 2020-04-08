package stopInObjectLiteralInInlineCallWithClosureInAnonymous

// FORCE_RANKING

fun main(args: Array<String>) {
    val a = 12

    {
        inlineF {
            val s = object : () -> Unit {
                override fun invoke() {
                    //Breakpoint!
                    nop(a)
                    nop(a)
                }
            }

            s()
        }
    }()
}

inline fun <R> inlineF(block: () -> R): R = block()

fun nop(a: Any) {}