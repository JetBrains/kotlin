interface I
interface J

fun foo(<caret>a: Any, b: String) {
    b.bar {
        this as J
    }
}

fun String.bar(init: I.() -> Unit) {}