// IS_APPLICABLE: false

inline fun <T, R> T.let(block: (T) -> R): R = block(this)

fun foo(arg: Any?): Int? {
    return arg?.let {
        <caret>x -> x.toString().let {
            x.hashCode() + it.hashCode()
        }
    }
}

