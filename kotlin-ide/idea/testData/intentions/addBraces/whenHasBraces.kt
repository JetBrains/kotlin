// IS_APPLICABLE: false

fun foo(a: Int) {
    when (a) {
        1 -> {
            foo(a)<caret>
        }
    }
}