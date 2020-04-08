// "Replace with label label@" "true"

fun run(block: () -> Unit) = block()

fun foo() {
    run @label<caret> {
        return@label
    }
}
