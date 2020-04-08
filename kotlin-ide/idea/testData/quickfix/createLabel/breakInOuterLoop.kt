// "Create label foo@" "true"

fun test() {
    while (true) {
        while (true) {
            break@<caret>foo
        }
    }
}