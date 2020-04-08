// "Simplify comparison" "true"
fun foo(x: Int, arg: Boolean) {
    if (arg && <caret>x != null) {

    }
}