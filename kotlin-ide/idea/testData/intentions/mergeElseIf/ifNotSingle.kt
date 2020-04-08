// IS_APPLICABLE: false

fun foo() {
    if (true) {

    } <caret>else {
        if (false) {
            foo()
        }
        val a = 5
    }
}