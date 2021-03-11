// IS_APPLICABLE: true

inline fun <T, R> T.let(block: (T) -> R): R = block(this)

fun foo(arg: Any?, y: Any?): Any? {
    return arg?.let {
        <caret>x -> x.toString().let { y }
    }
}

