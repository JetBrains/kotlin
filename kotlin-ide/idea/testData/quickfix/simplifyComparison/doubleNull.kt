// "Simplify comparison" "true"
fun foo(x: String?) {
    if (x == null) {

    }
    else {
        if (<caret>x == null) {
            bar()
        }
    }
}

fun bar() {}