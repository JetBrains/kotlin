// IS_APPLICABLE: false

fun parenPB(p: (() -> Unit) -> Unit): (() -> Unit) -> Unit = p

fun somethingNext(p: (() -> Unit) -> Unit) {
    <caret>(parenPB (p)) {}
}