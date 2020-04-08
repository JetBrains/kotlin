// IS_APPLICABLE: false

fun foo() {
    when (1) {
        else -> foo()<caret>
    }
}